/*
 * $Id$
 */

package com.p6majo.core.cas.poly;


import com.p6majo.core.cas.arith.*;
import com.p6majo.core.cas.exceptions.InvalidExpressionException;
import com.p6majo.core.cas.structure.RingElem;
import com.p6majo.core.cas.structure.RingFactory;
import com.p6majo.logger.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * GenPolynomial Tokenizer. Used to read rational polynomials and lists of
 * polynomials from input streams. Arbitrary polynomial rings and coefficient
 * rings can be read with RingFactoryTokenizer. <b>Note:</b> Can no more read
 * QuotientRing since end of 2010, revision 3441. Quotient coefficients and
 * others can still be read if the respective factory is provided via the
 * constructor.
 * @author Heinz Kredel
 */
public class GenPolynomialTokenizer {


    private static final Logger logger = new Logger(GenPolynomialTokenizer.class);


    private static final boolean debug = logger.isDebugEnabled();


    private String[] vars;


    private int nvars = 1;


    private TermOrder tord;



    private final StreamTokenizer tok;


    private final Reader reader;


    private RingFactory fac;


    private static enum coeffType {
        BigRat, BigInt, ModInt, BigC, BigQ, BigO, BigD, IntFunc
    };


    private coeffType parsedCoeff = coeffType.BigRat;


    private GenPolynomialRing pfac;


    private static enum polyType {
        PolBigRat, PolBigInt, PolModInt, PolBigC, PolBigD, PolBigQ, PolBigO, PolANrat, PolANmod, PolIntFunc
    };


    @SuppressWarnings("unused")
    private polyType parsedPoly = polyType.PolBigRat;



    /**
     * No-args constructor reads from System.in.
     */
    public GenPolynomialTokenizer() {
        this(new BufferedReader(new InputStreamReader(System.in, Charset.forName("UTF8"))));
    }


    /**
     * Constructor with Ring and Reader.
     * @param rf ring factory.
     * @param r reader stream.
     */
    public GenPolynomialTokenizer(GenPolynomialRing rf, Reader r) {
        this(r);
        if (rf == null) {
            return;
        }
        pfac = rf;
        fac = rf.coFac;
        vars = rf.vars;
        if (vars != null) {
            nvars = vars.length;
        }
        tord = rf.tord;
        // relation table

    }


    /**
     * Constructor with Reader.
     * @param r reader stream.
     */
    @SuppressWarnings("unchecked")
    public GenPolynomialTokenizer(Reader r) {
        //BasicConfigurator.configure();
        vars = null;
        tord = new TermOrder();
        nvars = 1;
        fac = new BigRational(1);

        pfac = new GenPolynomialRing<BigRational>(fac, nvars, tord, vars);

        reader = r;
        tok = new StreamTokenizer(reader);
        tok.resetSyntax();
        // tok.eolIsSignificant(true); no more
        tok.eolIsSignificant(false);
        tok.wordChars('0', '9');
        tok.wordChars('a', 'z');
        tok.wordChars('A', 'Z');
        tok.wordChars('_', '_'); // for subscripts x_i
        tok.wordChars('/', '/'); // wg. rational numbers
        tok.wordChars('.', '.'); // wg. floats
        tok.wordChars('~', '~'); // wg. quaternions
        tok.wordChars(128 + 32, 255);
        tok.whitespaceChars(0, ' ');
        tok.commentChar('#');
        tok.quoteChar('"');
        tok.quoteChar('\'');
        //tok.slashStarComments(true); does not work

    }


    /**
     * Initialize coefficient and polynomial factories.
     * @param rf ring factory.
     * @param ct coefficient type.
     */
    @SuppressWarnings("unchecked")
    public void initFactory(RingFactory rf, coeffType ct) {
        fac = rf;
        parsedCoeff = ct;

        switch (ct) {
        case BigRat:
            pfac = new GenPolynomialRing<BigRational>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigRat;
            break;
        case BigInt:
            pfac = new GenPolynomialRing<BigInteger>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigInt;
            break;
        case ModInt:
            pfac = new GenPolynomialRing<ModInteger>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolModInt;
            break;
        case BigC:
            pfac = new GenPolynomialRing<BigComplex>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigC;
            break;
        case BigQ:
            pfac = new GenPolynomialRing<BigQuaternion>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigQ;
            break;
        case BigO:
            pfac = new GenPolynomialRing<BigOctonion>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigO;
            break;
        case BigD:
            pfac = new GenPolynomialRing<BigDecimal>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigD;
            break;
        case IntFunc:
            pfac = new GenPolynomialRing<GenPolynomial<BigRational>>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolIntFunc;
            break;
        default:
            pfac = new GenPolynomialRing<BigRational>(fac, nvars, tord, vars);
            parsedPoly = polyType.PolBigRat;
        }
    }
    

    /**
     * Parsing method for GenPolynomial. Syntax depends also on the syntax of
     * the coefficients, as the respective parser is used. Basic term/monomial
     * syntax:
     * 
     * <pre>
    ... coefficient variable**exponent ... variable^exponent + ... - ....
     * </pre>
     * 
     * Juxtaposition means multiplication <code>*</code>. Then terms/monomials
     * can be added or subtracted <code>+, -</code> and grouped by parenthesis
     * <code>()</code>. There are some heuristics to detect when a coefficient
     * should be parsed. To force parsing of a coefficient enclose it in braces
     * <code>{}</code>.
     * @return the next polynomial.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public GenPolynomial nextPolynomial() throws IOException {
        if (debug) {
            logger.log(Logger.Level.debug,"torder = " + tord);
        }
        GenPolynomial a = pfac.getZERO();
        GenPolynomial a1 = pfac.getONE();
        ExpVector leer = pfac.evzero;

        if (debug) {
            logger.log(Logger.Level.debug,"a = " + a);
            logger.log(Logger.Level.debug,"a1 = " + a1);
        }
        GenPolynomial b = a1;
        GenPolynomial c;
        int tt; //, oldtt;
        //String rat = "";
        char first;
        RingElem r;
        ExpVector e;
        int ix;
        long ie;
        //boolean done = false;
        while (true) { //!done
            // next input. determine next action
            tt = tok.nextToken();
            //System.out.println("while tt = " + tok);
            if (debug) logger.log(Logger.Level.debug,"while tt = " + tok);
            if (tt == StreamTokenizer.TT_EOF)
                break;
            switch (tt) {
            case ')':
            case ',':
                return a; // do not change or remove
            case '-':
                b = b.negate();
            case '+':
            case '*':
                tt = tok.nextToken();
                break;
            default: // skip
            }
            // read coefficient, monic monomial and polynomial
            if (tt == StreamTokenizer.TT_EOF)
                break;
            switch (tt) {
            // case '_': removed 
            case '}':
                throw new InvalidExpressionException("mismatch of braces after " + a + ", error at " + b);
            case '{': // recursion
                StringBuffer rf = new StringBuffer();
                int level = 0;
                do {
                    tt = tok.nextToken();
                    //System.out.println("token { = " + ((char)tt) + ", " + tt + ", level = " + level);
                    if (tt == StreamTokenizer.TT_EOF) {
                        throw new InvalidExpressionException(
                                        "mismatch of braces after " + a + ", error at " + b);
                    }
                    if (tt == '{') {
                        level++;
                    }
                    if (tt == '}') {
                        level--;
                        if (level < 0) {
                            continue; // skip last closing brace 
                        }
                    }
                    if (tok.sval != null) {
                        if (rf.length() > 0 && rf.charAt(rf.length() - 1) != '.') {
                            rf.append(" ");
                        }
                        rf.append(tok.sval); // " " + 
                    } else {
                        rf.append((char) tt);
                    }
                } while (level >= 0);
                //System.out.println("coeff{} = " + rf.toString() );
                try {
                    r = (RingElem) fac.parse(rf.toString());
                } catch (NumberFormatException re) {
                    throw new InvalidExpressionException("not a number " + rf, re);
                }
                if (debug)
                    logger.log(Logger.Level.debug,"coeff " + r);
                ie = nextExponent();
                if (debug)
                    logger.log(Logger.Level.debug,"ie " + ie);
                r = (RingElem) r.power(ie); //Power.<RingElem> positivePower(r, ie);
                if (debug)
                    logger.log(Logger.Level.debug,"coeff^ie " + r);
                b = b.multiply(r, leer);
                tt = tok.nextToken();
                if (debug)
                    logger.log(Logger.Level.debug,"tt,digit = " + tok);
                //no break;
                break;

            //case '.': // eventually a float
            //System.out.println("start . = " + reader);
            //throw new InvalidExpressionException("float must start with a digit ");

            case StreamTokenizer.TT_WORD:
                //System.out.println("TT_WORD: " + tok.sval);
                if (tok.sval == null || tok.sval.length() == 0)
                    break;
                // read coefficient
                first = tok.sval.charAt(0);
                if (digit(first) || first == '/' || first == '.' || first == '~') {
                    //System.out.println("coeff 0 = " + tok.sval );
                    StringBuffer df = new StringBuffer();
                    df.append(tok.sval);
                    if (tok.sval.length() > 1 && digit(tok.sval.charAt(1))) {
                        //System.out.println("start / or . = " + tok.sval);
                        if (first == '/') { // let x/2 be x 1/2
                            df.insert(0, "1");
                        }
                        if (first == '.') { // let x.2 be x 0.2
                            df.insert(0, "0");
                        }
                    }
                    if (tok.sval.charAt(tok.sval.length() - 1) == 'i') { // complex number
                        tt = tok.nextToken();
                        if (debug)
                            logger.log(Logger.Level.debug,"tt,im = " + tok);
                        if (tok.sval != null || tt == '-') {
                            if (tok.sval != null) {
                                df.append(tok.sval);
                            } else {
                                df.append("-");
                            }
                            if (tt == '-') {
                                tt = tok.nextToken(); // todo: decimal number
                                if (tok.sval != null && digit(tok.sval.charAt(0))) {
                                    df.append(tok.sval);

                                } else {
                                    tok.pushBack();
                                }
                            }
                        } else {
                            tok.pushBack();
                        }
                    }
                    tt = tok.nextToken();
                    if (tt == '.') { // decimal number, obsolete by word char?
                        tt = tok.nextToken();
                        if (debug)
                            logger.log(Logger.Level.debug,"tt,dot = " + tok);
                        if (tok.sval != null) {
                            df.append(".");
                            df.append(tok.sval);
                        } else {
                            tok.pushBack();
                            tok.pushBack();
                        }
                    } else {
                        tok.pushBack();
                    }
                    try {
                        //System.out.println("df = " + df + ", fac = " + fac.getClass());
                        r = (RingElem) fac.parse(df.toString());
                        //System.out.println("r = " + r);
                    } catch (NumberFormatException re) {
                        //System.out.println("re = " + re);
                        throw new InvalidExpressionException("not a number " + df, re);
                    }
                    if (debug)
                        logger.log(Logger.Level.debug,"coeff " + r);
                    //System.out.println("r = " + r.toScriptFactory());
                    ie = nextExponent();
                    if (debug)
                        logger.log(Logger.Level.debug,"ie " + ie);
                    // r = r^ie;
                    r = (RingElem) r.power(ie); //Power.<RingElem> positivePower(r, ie);
                    if (debug)
                        logger.log(Logger.Level.debug,"coeff^ie " + r);
                    b = b.multiply(r, leer);
                    tt = tok.nextToken();
                    if (debug)
                        logger.log(Logger.Level.debug,"tt,digit = " + tok);
                }
                if (tt == StreamTokenizer.TT_EOF)
                    break;
                if (tok.sval == null)
                    break;
                // read monomial or recursion 
                first = tok.sval.charAt(0);
                if (letter(first)) {
                    ix = leer.indexVar(tok.sval, vars); //indexVar( tok.sval );
                    if (ix < 0) { // not found
                        try {
                            r = (RingElem) fac.parse(tok.sval);
                        } catch (NumberFormatException re) {
                            throw new InvalidExpressionException("recursively unknown variable " + tok.sval);
                        }
                        if (debug)
                            logger.log(Logger.Level.info,"coeff " + r);
                        //if (r.isONE() || r.isZERO()) {
                        //com.p6majo.logger.log(Logger.Level.error,"Unknown varibable " + tok.sval);
                        //done = true;
                        //break;
                        //throw new InvalidExpressionException("recursively unknown variable " + tok.sval);
                        //}
                        ie = nextExponent();
                        //  System.out.println("ie: " + ie);
                        r = (RingElem) r.power(ie); //Power.<RingElem> positivePower(r, ie);
                        b = b.multiply(r);
                    } else { // found
                        //  System.out.println("ix: " + ix);
                        ie = nextExponent();
                        //  System.out.println("ie: " + ie);
                        e = ExpVector.create(vars.length, ix, ie);
                        b = b.multiply(e);
                    }
                    tt = tok.nextToken();
                    if (debug)
                        logger.log(Logger.Level.debug,"tt,letter = " + tok);
                }
                break;

            case '(':
                c = nextPolynomial();
                if (debug)
                    logger.log(Logger.Level.debug,"factor " + c);
                ie = nextExponent();
                if (debug)
                    logger.log(Logger.Level.debug,"ie " + ie);
                c = (GenPolynomial) c.power(ie); //Power.<GenPolynomial> positivePower(c, ie);
                if (debug)
                    logger.log(Logger.Level.debug,"factor^ie " + c);
                b = b.multiply(c);
                tt = tok.nextToken();
                if (debug)
                    logger.log(Logger.Level.debug,"tt,digit = " + tok);
                //no break;
                break;

            default: //skip 
            }
            //if (done)
            //    break; // unknown variable
            if (tt == StreamTokenizer.TT_EOF)
                break;
            // complete polynomial
            tok.pushBack();
            switch (tt) {
            case '-':
            case '+':
            case ')':
            case ',':
                if (debug) logger.log(Logger.Level.debug,"b, = " + b);
                a = a.sum(b);
                b = a1;
                break;
            case '*':
                if (debug) logger.log(Logger.Level.debug,"b, = " + b);
                //a = a.sum(b); 
                //b = a1;
                break;
            case '\n':
                tt = tok.nextToken();
                if (debug)
                    logger.log(Logger.Level.debug,"tt,nl = " + tt);
                break;
            default: // skip or finish ?
                if (debug)
                    logger.log(Logger.Level.debug,"default: " + tok);
            }
        }
        if (debug)
            logger.log(Logger.Level.debug,"b = " + b);
        a = a.sum(b);
        if (debug) logger.log(Logger.Level.debug,"a = " + a);
        // b = a1;
        return a;
    }


    /**
     * Parsing method for exponent (of variable). Syntax:
     * 
     * <pre>
     * ^long | **long
     * </pre>
     * 
     * @return the next exponent or 1.
     * @throws IOException
     */
    public long nextExponent() throws IOException {
        long e = 1;
        char first;
        int tt;
        tt = tok.nextToken();
        if (tt == '^') {
            if (debug)
                logger.log(Logger.Level.debug,"exponent ^");
            tt = tok.nextToken();
            if (tok.sval != null) {
                first = tok.sval.charAt(0);
                if (digit(first)) {
                    e = Long.parseLong(tok.sval);
                    return e;
                }
            }
        }
        if (tt == '*') {
            tt = tok.nextToken();
            if (tt == '*') {
                if (debug)
                    logger.log(Logger.Level.debug,"exponent **");
                tt = tok.nextToken();
                if (tok.sval != null) {
                    first = tok.sval.charAt(0);
                    if (digit(first)) {
                        e = Long.parseLong(tok.sval);
                        return e;
                    }
                }
            }
            tok.pushBack();
        }
        tok.pushBack();
        return e;
    }


    /**
     * Parsing method for comments. Syntax:
     * 
     * <pre>
     * (* comment *) | /_* comment *_/
     * </pre>
     * 
     * without <code>_</code>. Unused, as it does not work with this pushBack().
     */
    public String nextComment() throws IOException {
        // syntax: (* comment *) | /* comment */ 
        StringBuffer c = new StringBuffer();
        int tt;
        if (debug)
            logger.log(Logger.Level.debug,"comment: " + tok);
        tt = tok.nextToken();
        if (debug)
            logger.log(Logger.Level.debug,"comment: " + tok);
        if (tt == '(') {
            tt = tok.nextToken();
            if (debug)
                logger.log(Logger.Level.debug,"comment: " + tok);
            if (tt == '*') {
                if (debug)
                    logger.log(Logger.Level.debug,"comment: ");
                while (true) {
                    tt = tok.nextToken();
                    if (tt == '*') {
                        tt = tok.nextToken();
                        if (tt == ')') {
                            return c.toString();
                        }
                        tok.pushBack();
                    }
                    c.append(tok.sval);
                }
            }
            tok.pushBack();
            if (debug)
                logger.log(Logger.Level.debug,"comment: " + tok);
        }
        tok.pushBack();
        if (debug)
            logger.log(Logger.Level.debug,"comment: " + tok);
        return c.toString();
    }


    /**
     * Parsing method for variable list. Syntax:
     * 
     * <pre>
     * (a, b c, de)
     * </pre>
     * 
     * gives <code>[ "a", "b", "c", "de" ]</code>
     * @return the next variable list.
     * @throws IOException
     */
    public String[] nextVariableList() throws IOException {
        List<String> l = new ArrayList<String>();
        int tt;
        tt = tok.nextToken();
        //System.out.println("vList tok = " + tok);
        if (tt == '(' || tt == '{') {
            if (debug) logger.log(Logger.Level.debug,"variable list");
            tt = tok.nextToken();
            while (true) {
                if (tt == StreamTokenizer.TT_EOF)
                    break;
                if (tt == ')' || tt == '}')
                    break;
                if (tt == StreamTokenizer.TT_WORD) {
                    //System.out.println("TT_WORD: " + tok.sval);
                    l.add(tok.sval);
                }
                tt = tok.nextToken();
            }
        } else {
            tok.pushBack();
        }
        Object[] ol = l.toArray();
        String[] v = new String[ol.length];
        for (int i = 0; i < v.length; i++) {
            v[i] = (String) ol[i];
        }
        return v;
    }


    /**
     * Parsing method for coefficient ring. Syntax:
     * 
     * <pre>
     * Rat | Q | Int | Z | Mod modul | Complex | C | D | Quat | AN[ (var) ( poly ) ] | AN[ modul (var) ( poly ) ] | IntFunc (var_list)
     * </pre>
     * 
     * @return the next coefficient factory.
     * @throws IOException
     */
    @SuppressWarnings({ "unchecked", "cast" })
    public RingFactory nextCoefficientRing() throws IOException {
        RingFactory coeff = null;
        coeffType ct = null;
        int tt;
        tt = tok.nextToken();
        if (tok.sval != null) {
            if (tok.sval.equalsIgnoreCase("Q")) {
                coeff = new BigRational(0);
                ct = coeffType.BigRat;
            } else if (tok.sval.equalsIgnoreCase("Rat")) {
                coeff = new BigRational(0);
                ct = coeffType.BigRat;
            } else if (tok.sval.equalsIgnoreCase("D")) {
                coeff = new BigDecimal(0);
                ct = coeffType.BigD;
            } else if (tok.sval.equalsIgnoreCase("Z")) {
                coeff = new BigInteger(0);
                ct = coeffType.BigInt;
            } else if (tok.sval.equalsIgnoreCase("Int")) {
                coeff = new BigInteger(0);
                ct = coeffType.BigInt;
            } else if (tok.sval.equalsIgnoreCase("C")) {
                coeff = new BigComplex(0);
                ct = coeffType.BigC;
            } else if (tok.sval.equalsIgnoreCase("Complex")) {
                coeff = new BigComplex(0);
                ct = coeffType.BigC;
            } else if (tok.sval.equalsIgnoreCase("Quat")) {
                logger.log(Logger.Level.warning,"parse of quaternion coefficients may fail for negative components (use ~ for -)");
                coeff = new BigQuaternionRing();
                ct = coeffType.BigQ;
            } else if (tok.sval.equalsIgnoreCase("Oct")) {
                logger.log(Logger.Level.warning,"parse of octonion coefficients may fail for negative components (use ~ for -)");
                coeff = new BigOctonion(new BigQuaternionRing());
                ct = coeffType.BigO;
            } else if (tok.sval.equalsIgnoreCase("Mod")) {
                tt = tok.nextToken();
                boolean openb = false;
                if (tt == '[') { // optional
                    openb = true;
                    tt = tok.nextToken();
                }
                if (tok.sval != null && tok.sval.length() > 0) {
                    if (digit(tok.sval.charAt(0))) {
                        BigInteger mo = new BigInteger(tok.sval);
                        BigInteger lm = new BigInteger(ModLongRing.MAX_LONG); //wrong: Long.MAX_VALUE);
                        if (mo.compareTo(lm) < 0) {
                            coeff = new ModLongRing(mo.getVal());
                        } else {
                            coeff = new ModIntegerRing(mo.getVal());
                        }
                        //System.out.println("coeff = " + coeff + " :: " + coeff.getClass());
                        ct = coeffType.ModInt;
                    } else {
                        tok.pushBack();
                    }
                } else {
                    tok.pushBack();
                }
                if (tt == ']' && openb) { // optional
                    tt = tok.nextToken();
                }
            } else if (tok.sval.equalsIgnoreCase("RatFunc") || tok.sval.equalsIgnoreCase("ModFunc")) {
                //com.p6majo.logger.log(Logger.Level.error,"RatFunc and ModFunc can no more be read, see edu.jas.application.RingFactoryTokenizer.");
                throw new InvalidExpressionException(
                                "RatFunc and ModFunc can no more be read, see edu.jas.application.RingFactoryTokenizer.");
            } else if (tok.sval.equalsIgnoreCase("IntFunc")) {
                String[] rfv = nextVariableList();
                //System.out.println("rfv = " + rfv.length + " " + rfv[0]);
                int vr = rfv.length;
                BigRational bi = new BigRational();
                TermOrder to = new TermOrder(TermOrder.INVLEX);
                GenPolynomialRing<BigRational> pcf = new GenPolynomialRing<BigRational>(bi, vr, to, rfv);
                coeff = pcf;
                ct = coeffType.IntFunc;
            } else if (tok.sval.equalsIgnoreCase("AN")) {
                tt = tok.nextToken();
                if (tt == '[') {
                    tt = tok.nextToken();
                    RingFactory tcfac = new ModIntegerRing("19");
                    if (tok.sval != null && tok.sval.length() > 0) {
                        if (digit(tok.sval.charAt(0))) {
                            tcfac = new ModIntegerRing(tok.sval);
                        } else {
                            tcfac = new BigRational();
                            tok.pushBack();
                        }
                    } else {
                        tcfac = new BigRational();
                        tok.pushBack();
                    }
                    String[] anv = nextVariableList();
                    //System.out.println("anv = " + anv.length + " " + anv[0]);
                    int vs = anv.length;
                    if (vs != 1) {
                        throw new InvalidExpressionException(
                                        "AlgebraicNumber only for univariate polynomials "
                                                        + Arrays.toString(anv));
                    }
                    String[] ovars = vars;
                    vars = anv;
                    GenPolynomialRing tpfac = pfac;
                    RingFactory tfac = fac;
                    fac = tcfac;
                    // pfac and fac used in nextPolynomial()
                    if (tcfac instanceof ModIntegerRing) {
                        pfac = new GenPolynomialRing<ModInteger>(tcfac, vs, new TermOrder(), anv);
                    } else {
                        pfac = new GenPolynomialRing<BigRational>(tcfac, vs, new TermOrder(), anv);
                    }
                    if (debug) {
                        logger.log(Logger.Level.debug,"pfac = " + pfac);
                    }
                    tt = tok.nextToken();
                    GenPolynomial mod;
                    if (tt == '(') {
                        mod = nextPolynomial();
                        tt = tok.nextToken();
                        if (tok.ttype != ')')
                            tok.pushBack();
                    } else {
                        tok.pushBack();
                        mod = nextPolynomial();
                    }
                    if (debug) {
                        logger.log(Logger.Level.debug,"mod = " + mod);
                    }
                    pfac = tpfac;
                    fac = tfac;
                    vars = ovars;
                    if (debug) {
                        logger.log(Logger.Level.debug,"coeff = " + coeff);
                    }
                    tt = tok.nextToken();
                    if (tt == ']') {
                        //ok, no nextToken();
                    } else {
                        tok.pushBack();
                    }
                } else {
                    tok.pushBack();
                }
            }
        }
        if (coeff == null) {
            tok.pushBack();
            coeff = new BigRational();
            ct = coeffType.BigRat;
        }
        parsedCoeff = ct;
        return coeff;
    }


    /**
     * Parsing method for weight list. Syntax:
     * 
     * <pre>
     * (w1, w2, w3, ..., wn)
     * </pre>
     * 
     * @return the next weight list.
     * @throws IOException
     */
    public long[] nextWeightList() throws IOException {
        List<Long> l = new ArrayList<Long>();
        long e;
        char first;
        int tt;
        tt = tok.nextToken();
        if (tt == '(') {
            if (debug) logger.log(Logger.Level.debug,"weight list");
            tt = tok.nextToken();
            while (true) {
                if (tt == StreamTokenizer.TT_EOF)
                    break;
                if (tt == ')')
                    break;
                if (tok.sval != null) {
                    first = tok.sval.charAt(0);
                    if (digit(first)) {
                        e = Long.parseLong(tok.sval);
                        l.add(Long.valueOf(e));
                        //System.out.println("w: " + e);
                    }
                }
                tt = tok.nextToken(); // also comma
            }
        } else {
            tok.pushBack();
        }
        Long[] ol = new Long[1];
        ol = l.toArray(ol);
        long[] w = new long[ol.length];
        for (int i = 0; i < w.length; i++) {
            w[i] = ol[ol.length - i - 1].longValue();
        }
        return w;
    }


    /**
     * Parsing method for weight array. Syntax:
     * 
     * <pre>
     * ( (w11, ...,w1n), ..., (wm1, ..., wmn) )
     * </pre>
     * 
     * @return the next weight array.
     * @throws IOException
     */
    public long[][] nextWeightArray() throws IOException {
        List<long[]> l = new ArrayList<long[]>();
        long[] e;
        char first;
        int tt;
        tt = tok.nextToken();
        if (tt == '(') {
            if (debug) logger.log(Logger.Level.debug,"weight array");
            tt = tok.nextToken();
            while (true) {
                if (tt == StreamTokenizer.TT_EOF)
                    break;
                if (tt == ')')
                    break;
                if (tt == '(') {
                    tok.pushBack();
                    e = nextWeightList();
                    l.add(e);
                    //System.out.println("wa: " + e);
                } else if (tok.sval != null) {
                    first = tok.sval.charAt(0);
                    if (digit(first)) {
                        tok.pushBack();
                        tok.pushBack();
                        e = nextWeightList();
                        l.add(e);
                        break;
                        //System.out.println("w: " + e);
                    }
                }
                tt = tok.nextToken(); // also comma
            }
        } else {
            tok.pushBack();
        }
        Object[] ol = l.toArray();
        long[][] w = new long[ol.length][];
        for (int i = 0; i < w.length; i++) {
            w[i] = (long[]) ol[i];
        }
        return w;
    }


    /**
     * Parsing method for split index. Syntax:
     * 
     * <pre>
     * |i|
     * </pre>
     * 
     * @return the next split index.
     * @throws IOException
     */
    public int nextSplitIndex() throws IOException {
        int e = -1; // =unknown
        int e0 = -1; // =unknown
        char first;
        int tt;
        tt = tok.nextToken();
        if (tt == '|') {
            if (debug) {
                logger.log(Logger.Level.debug,"split index");
            }
            tt = tok.nextToken();
            if (tt == StreamTokenizer.TT_EOF) {
                return e;
            }
            if (tok.sval != null) {
                first = tok.sval.charAt(0);
                if (digit(first)) {
                    e = Integer.parseInt(tok.sval);
                    //System.out.println("w: " + i);
                }
                tt = tok.nextToken();
                if (tt != '|') {
                    tok.pushBack();
                }
            }
        } else if (tt == '[') {
            if (debug) {
                logger.log(Logger.Level.debug,"split index");
            }
            tt = tok.nextToken();
            if (tt == StreamTokenizer.TT_EOF) {
                return e;
            }
            if (tok.sval != null) {
                first = tok.sval.charAt(0);
                if (digit(first)) {
                    e0 = Integer.parseInt(tok.sval);
                    //System.out.println("w: " + i);
                }
                tt = tok.nextToken();
                if (tt == ',') {
                    tt = tok.nextToken();
                    if (tt == StreamTokenizer.TT_EOF) {
                        return e0;
                    }
                    if (tok.sval != null) {
                        first = tok.sval.charAt(0);
                        if (digit(first)) {
                            e = Integer.parseInt(tok.sval);
                            //System.out.println("w: " + i);
                        }
                    }
                    if (tt != ']') {
                        tok.pushBack();
                    }
                }
            }
        } else {
            tok.pushBack();
        }
        return e;
    }


    /**
     * Parsing method for term order name. Syntax:
     * 
     * <pre>
     * L | IL | LEX | G | IG | GRLEX | W(weights) | '|'split index'|'
     * </pre>
     * 
     * @return the next term order.
     * @throws IOException
     */
    public TermOrder nextTermOrder() throws IOException {
        int evord = TermOrder.DEFAULT_EVORD;
        int tt;
        tt = tok.nextToken();
        if (tt == StreamTokenizer.TT_EOF) { /* nop */
        } else if (tt == StreamTokenizer.TT_WORD) {
            // System.out.println("TT_WORD: " + tok.sval);
            if (tok.sval != null) {
                if (tok.sval.equalsIgnoreCase("L")) {
                    evord = TermOrder.INVLEX;
                } else if (tok.sval.equalsIgnoreCase("IL")) {
                    evord = TermOrder.INVLEX;
                } else if (tok.sval.equalsIgnoreCase("INVLEX")) {
                    evord = TermOrder.INVLEX;
                } else if (tok.sval.equalsIgnoreCase("LEX")) {
                    evord = TermOrder.LEX;
                } else if (tok.sval.equalsIgnoreCase("G")) {
                    evord = TermOrder.IGRLEX;
                } else if (tok.sval.equalsIgnoreCase("IG")) {
                    evord = TermOrder.IGRLEX;
                } else if (tok.sval.equalsIgnoreCase("IGRLEX")) {
                    evord = TermOrder.IGRLEX;
                } else if (tok.sval.equalsIgnoreCase("GRLEX")) {
                    evord = TermOrder.GRLEX;
                } else if (tok.sval.equalsIgnoreCase("REVITDG")) {
                    evord = TermOrder.REVITDG;
                } else if (tok.sval.equalsIgnoreCase("REVILEX")) {
                    evord = TermOrder.REVILEX;
                } else if (tok.sval.equalsIgnoreCase("W")) {
                    long[][] w = nextWeightArray();
                    return new TermOrder(w);
                }
            }
        } else {
            tok.pushBack();
        }
        int s = nextSplitIndex();
        if (s <= 0) {
            return new TermOrder(evord);
        }
        return new TermOrder(evord, evord, nvars, s);
    }


    /**
     * Parsing method for polynomial list. Syntax:
     * 
     * <pre>
     * ( p1, p2, p3, ..., pn )
     * </pre>
     * 
     * @return the next polynomial list.
     * @throws IOException
     */
    public List<GenPolynomial> nextPolynomialList() throws IOException {
        GenPolynomial a;
        List<GenPolynomial> L = new ArrayList<GenPolynomial>();
        int tt;
        tt = tok.nextToken();
        if (tt == StreamTokenizer.TT_EOF)
            return L;
        if (tt != '(')
            return L;
        if (debug) logger.log(Logger.Level.debug,"polynomial list");
        while (true) {
            tt = tok.nextToken();
            if (tok.ttype == ',')
                continue;
            if (tt == '(') {
                a = nextPolynomial();
                tt = tok.nextToken();
                if (tok.ttype != ')')
                    tok.pushBack();
            } else {
                tok.pushBack();
                a = nextPolynomial();
            }
            logger.log(Logger.Level.info,"next pol = " + a);
            L.add(a);
            if (tok.ttype == StreamTokenizer.TT_EOF)
                break;
            if (tok.ttype == ')')
                break;
        }
        return L;
    }


    /**
     * Parsing method for submodule list. Syntax:
     * 
     * <pre>
     * ( ( p11, p12, p13, ..., p1n ), ..., ( pm1, pm2, pm3, ..., pmn ) )
     * </pre>
     * 
     * @return the next list of polynomial lists.
     * @throws IOException
     */
    public List<List<GenPolynomial>> nextSubModuleList() throws IOException {
        List<List<GenPolynomial>> L = new ArrayList<List<GenPolynomial>>();
        int tt;
        tt = tok.nextToken();
        if (tt == StreamTokenizer.TT_EOF)
            return L;
        if (tt != '(')
            return L;
        if (debug) logger.log(Logger.Level.debug,"module list");
        List<GenPolynomial> v = null;
        while (true) {
            tt = tok.nextToken();
            if (tok.ttype == ',')
                continue;
            if (tok.ttype == ')')
                break;
            if (tok.ttype == StreamTokenizer.TT_EOF)
                break;
            if (tt == '(') {
                tok.pushBack();
                v = nextPolynomialList();
                logger.log(Logger.Level.info,"next vect = " + v);
                L.add(v);
            }
        }
        return L;
    }


    /**
     * Parsing method for polynomial set. Syntax:
     * 
     * <pre>
     * coeffRing varList termOrderName polyList
     * </pre>
     * 
     * @return the next polynomial set.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public PolynomialList nextPolynomialSet() throws IOException {
        //String comments = "";
        //comments += nextComment();
        //if (debug) com.p6majo.logger.log(Logger.Level.debug,"comment = " + comments);

        RingFactory coeff = nextCoefficientRing();
        logger.log(Logger.Level.info,"coeff = " + coeff.getClass().getSimpleName());

        vars = nextVariableList();
        logger.log(Logger.Level.info,"vars = " + Arrays.toString(vars));
        if (vars != null) {
            nvars = vars.length;
        }

        tord = nextTermOrder();
        logger.log(Logger.Level.info,"tord = " + tord);
        // check more TOs

        initFactory(coeff, parsedCoeff); // global: nvars, tord, vars
        List<GenPolynomial> s = null;
        s = nextPolynomialList();
        logger.log(Logger.Level.info,"s = " + s);
        // comments += nextComment();
        return new PolynomialList(pfac, s);
    }


    /**
     * Parsing method for module set. Syntax:
     * 
     * <pre>
     * coeffRing varList termOrderName moduleList
     * </pre>
     * 
     * @return the next module set.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    public ModuleList nextSubModuleSet() throws IOException {
        //String comments = "";
        //comments += nextComment();
        //if (debug) com.p6majo.logger.log(Logger.Level.debug,"comment = " + comments);

        RingFactory coeff = nextCoefficientRing();
        logger.log(Logger.Level.info,"coeff = " + coeff.getClass().getSimpleName());

        vars = nextVariableList();
        logger.log(Logger.Level.info,"vars = " + Arrays.toString(vars));
        if (vars != null) {
            nvars = vars.length;
        }

        tord = nextTermOrder();
        logger.log(Logger.Level.info,"tord = " + tord);
        // check more TOs

        initFactory(coeff, parsedCoeff); // global: nvars, tord, vars
        List<List<GenPolynomial>> m = null;
        m = nextSubModuleList();
        logger.log(Logger.Level.info,"m = " + m);
        // comments += nextComment();

        return new ModuleList(pfac, m);
    }





    // must also allow +/- // does not work with tokenizer
    //private static boolean number(char x) {
    //    return digit(x) || x == '-' || x == '+';
    //}


    static boolean digit(char x) {
        return '0' <= x && x <= '9';
    }


    static boolean letter(char x) {
        return ('a' <= x && x <= 'z') || ('A' <= x && x <= 'Z');
    }


    // unused
    public void nextComma() throws IOException {
        int tt;
        if (tok.ttype == ',') {
            tt = tok.nextToken();
            if (debug) {
                logger.log(Logger.Level.debug,"after comma: " + tt);
            }
        }
    }


    /**
     * Parse variable list from String.
     * @param s String. Syntax:
     * 
     *            <pre>
     * (n1,...,nk)
     *            </pre>
     * 
     *            or
     * 
     *            <pre>
     * (n1 ... nk)
     *            </pre>
     * 
     *            parenthesis are optional.
     * @return array of variable names found in s.
     */
    public static String[] variableList(String s) {
        String[] vl = null;
        if (s == null) {
            return vl;
        }
        String st = s.trim();
        if (st.length() == 0) {
            return new String[0];
        }
        if (st.charAt(0) == '(') {
            st = st.substring(1);
        }
        if (st.charAt(st.length() - 1) == ')') {
            st = st.substring(0, st.length() - 1);
        }
        st = st.replaceAll(",", " ");
        List<String> sl = new ArrayList<String>();
        Scanner sc = new Scanner(st);
        while (sc.hasNext()) {
            String sn = sc.next();
            sl.add(sn);
        }
        sc.close();
        vl = new String[sl.size()];
        int i = 0;
        for (String si : sl) {
            vl[i] = si;
            i++;
        }
        return vl;
    }


    /**
     * Extract variable list from expression.
     * @param s String. Syntax: any polynomial expression.
     * @return array of variable names found in s.
     */
    public static String[] expressionVariables(String s) {
        String[] vl = null;
        if (s == null) {
            return vl;
        }
        String st = s.trim();
        if (st.length() == 0) {
            return new String[0];
        }
        st = st.replaceAll(",", " ");
        st = st.replaceAll("\\+", " ");
        st = st.replaceAll("-", " ");
        st = st.replaceAll("\\*", " ");
        st = st.replaceAll("/", " ");
        st = st.replaceAll("\\(", " ");
        st = st.replaceAll("\\)", " ");
        st = st.replaceAll("\\{", " ");
        st = st.replaceAll("\\}", " ");
        st = st.replaceAll("\\[", " ");
        st = st.replaceAll("\\]", " ");
        st = st.replaceAll("\\^", " ");
        //System.out.println("st = " + st);

        Set<String> sl = new TreeSet<String>();
        Scanner sc = new Scanner(st);
        while (sc.hasNext()) {
            String sn = sc.next();
            if (sn == null || sn.length() == 0) {
                continue;
            }
            //System.out.println("sn = " + sn);
            int i = 0;
            while (digit(sn.charAt(i)) && i < sn.length() - 1) {
                i++;
            }
            //System.out.println("sn = " + sn + ", i = " + i);
            if (i > 0) {
                sn = sn.substring(i, sn.length());
            }
            //System.out.println("sn = " + sn);
            if (sn.length() == 0) {
                continue;
            }
            if (!letter(sn.charAt(0))) {
                continue;
            }
            //System.out.println("sn = " + sn);
            sl.add(sn);
        }
        sc.close();
        vl = new String[sl.size()];
        int i = 0;
        for (String si : sl) {
            vl[i] = si;
            i++;
        }
        return vl;
    }

}
