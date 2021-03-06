/*
 * $Id$
 */

package com.p6majo.core.cas.poly;


import com.p6majo.core.cas.kern.Scripting;
import com.p6majo.core.cas.structure.RingElem;
import com.p6majo.core.cas.vector.GenVector;
import com.p6majo.core.cas.vector.GenVectorModul;
import com.p6majo.logger.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * List of vectors of polynomials. Mainly for storage and printing / toString
 * and conversions to other representations.
 * @author Heinz Kredel
 */

public class ModuleList<C extends RingElem<C>> implements Serializable {


    /**
     * The factory for the solvable polynomial ring.
     */
    public final GenPolynomialRing<C> ring;


    /**
     * The data structure is a List of Lists of polynomials.
     */
    public final List<List<GenPolynomial<C>>> list;


    /**
     * Number of rows in the data structure.
     */
    public final int rows; // -1 is undefined


    /**
     * Number of columns in the data structure.
     */
    public final int cols; // -1 is undefined


    private static final Logger logger = new Logger(ModuleList.class);


    /**
     * Constructor.
     * @param r polynomial ring factory.
     * @param l list of list of polynomials.
     */
    public ModuleList(GenPolynomialRing<C> r, List<List<GenPolynomial<C>>> l) {
        ring = r;
        list = ModuleList.<C> padCols(r, l);
        if (list == null) {
            rows = -1;
            cols = -1;
        } else {
            rows = list.size();
            if (rows > 0) {
                cols = list.get(0).size();
            } else {
                cols = -1;
            }
        }
    }



    /**
     * Constructor.
     * @param r polynomial ring factory.
     * @param l list of vectors of polynomials.
     */
    public ModuleList(GenVectorModul<GenPolynomial<C>> r, List<GenVector<GenPolynomial<C>>> l) {
        this((GenPolynomialRing<C>) r.coFac, ModuleList.<C> vecToList(l));
    }


    /**
     * Comparison with any other object.
     * @see Object#equals(Object)
     */
    @Override
    @SuppressWarnings("unchecked")
    // not jet working
    public boolean equals(Object m) {
        if (m == null) {
            return false;
        }
        if (!(m instanceof ModuleList)) {
            //System.out.println("ModuleList");
            return false;
        }
        ModuleList<C> ml = (ModuleList<C>) m;
        if (!ring.equals(ml.ring)) {
            //System.out.println("Ring");
            return false;
        }
        if (list == ml.list) {
            return true;
        }
        if (list == null || ml.list == null) {
            //System.out.println("List, null");
            return false;
        }
        if (list.size() != ml.list.size()) {
            //System.out.println("List, size");
            return false;
        }
        // compare sorted lists
        List otl = OrderedModuleList.sort(ring, list);
        List oml = OrderedModuleList.sort(ring, ml.list);
        if (!otl.equals(oml)) {
            return false;
        }
        return true;
    }


    /**
     * Hash code for this module list.
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int h;
        h = ring.hashCode();
        h = 37 * h + (list == null ? 0 : list.hashCode());
        return h;
    }


    /**
     * String representation of the module list.
     * @see Object#toString()
     */
    @Override
    //@SuppressWarnings("unchecked") 
    public String toString() {
        StringBuffer erg = new StringBuffer();
        String[] vars = null;
        if (ring != null) {
            erg.append(ring.toString());
            vars = ring.getVars();
        }
        if (list == null) {
            erg.append(")");
            return erg.toString();
        }
        boolean first = true;
        erg.append("(\n");
        for (List<GenPolynomial<C>> row : list) {
            if (first) {
                first = false;
            } else {
                erg.append(",\n");
            }
            boolean ifirst = true;
            erg.append(" ( ");
            String os;
            for (GenPolynomial<C> oa : row) {
                if (oa == null) {
                    os = "0";
                } else if (vars != null) {
                    os = oa.toString(vars);
                } else {
                    os = oa.toString();
                }
                if (ifirst) {
                    ifirst = false;
                } else {
                    erg.append(", ");
                    if (os.length() > 100) {
                        erg.append("\n");
                    }
                }
                erg.append(os);
            }
            erg.append(" )");
        }
        erg.append("\n)");
        return erg.toString();
    }


    /**
     * Get a scripting compatible string representation.
     * @return script compatible representation for this ModuleList.
     */
    public String toScript() {
        StringBuffer s = new StringBuffer();
         switch (Scripting.getLang()) {
        case Ruby:
            s.append("SubModule.new(");
            break;
        case Python:
        default:
            s.append("SubModule(");
        }
        if (ring != null) {
            s.append(ring.toScript());
        }
        if (list == null) {
            s.append(")");
            return s.toString();
        }
        switch (Scripting.getLang()) {
        case Ruby:
            s.append(",\"\",[");
            break;
        case Python:
        default:
            s.append(",list=[");
        }
        boolean first = true;
        for (List<GenPolynomial<C>> row : list) {
            if (first) {
                first = false;
            } else {
                s.append(",");
            }
            boolean ifirst = true;
            s.append(" ( ");
            String os;
            for (GenPolynomial<C> oa : row) {
                if (oa == null) {
                    os = "0";
                } else {
                    os = oa.toScript();
                }
                if (ifirst) {
                    ifirst = false;
                } else {
                    s.append(", ");
                }
                s.append(os);
            }
            s.append(" )");
        }
        s.append(" ])");
        return s.toString();
    }


    /**
     * Pad columns and remove zero rows. Make all rows have the same number of
     * columns.
     * @param ring polynomial ring factory.
     * @param l list of list of polynomials.
     * @return list of list of polynomials with same number of colums.
     */
    public static <C extends RingElem<C>> List<List<GenPolynomial<C>>> padCols(GenPolynomialRing<C> ring,
                    List<List<GenPolynomial<C>>> l) {
        if (l == null) {
            return l;
        }
        int mcols = 0;
        int rs = 0;
        for (List<GenPolynomial<C>> row : l) {
            if (row != null) {
                rs++;
                if (row.size() > mcols) {
                    mcols = row.size();
                }
            }
        }
        List<List<GenPolynomial<C>>> norm = new ArrayList<List<GenPolynomial<C>>>(rs);
        for (List<GenPolynomial<C>> row : l) {
            if (row != null) {
                List<GenPolynomial<C>> rn = new ArrayList<GenPolynomial<C>>(row);
                while (rn.size() < mcols) {
                    rn.add(ring.getZERO());
                }
                norm.add(rn);
            }
        }
        return norm;
    }


    /**
     * Get PolynomialList. Embed module in a polynomial ring.
     * @return polynomial list corresponding to this.
     */
    public PolynomialList<C> getPolynomialList() {
        GenPolynomialRing<C> pfac = ring.extend(cols);
        logger.log(Logger.Level.debug,"extended ring = " + pfac);
        //System.out.println("extended ring = " + pfac);

        List<GenPolynomial<C>> pols = null;
        if (list == null) { // rows < 0
            return new PolynomialList<C>(pfac, pols);
        }
        pols = new ArrayList<GenPolynomial<C>>(rows);
        if (rows == 0) { // nothing to do
            return new PolynomialList<C>(pfac, pols);
        }

        GenPolynomial<C> zero = pfac.getZERO();
        GenPolynomial<C> d = null;
        for (List<GenPolynomial<C>> r : list) {
            GenPolynomial<C> ext = zero;
            //int m = cols-1;
            int m = 0;
            for (GenPolynomial<C> c : r) {
                d = c.extend(pfac, m, 1l);
                ext = ext.sum(d);
                m++;
            }
            pols.add(ext);
        }
        return new PolynomialList<C>(pfac, pols);
    }

    /**
     * Get a list of vectors as List of list of GenPolynomials.
     * @param vlist list of vectors of polynomials.
     * @return list of polynomial lists from vlist.
     */
    public static <C extends RingElem<C>> List<List<GenPolynomial<C>>> vecToList(
                    List<GenVector<GenPolynomial<C>>> vlist) {
        List<List<GenPolynomial<C>>> list = null;
        if (vlist == null) {
            return list;
        }
        list = new ArrayList<List<GenPolynomial<C>>>(vlist.size());
        for (GenVector<GenPolynomial<C>> srow : vlist) {
            List<GenPolynomial<C>> row = srow.val;
            list.add(row);
        }
        return list;
    }

}
