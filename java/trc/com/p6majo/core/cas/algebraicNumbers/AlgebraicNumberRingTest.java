package trc.com.p6majo.core.cas.algebraicNumbers;

import com.p6majo.core.cas.algebraicNumbers.AlgebraicNumber;
import com.p6majo.core.cas.algebraicNumbers.AlgebraicNumberRing;
import com.p6majo.core.cas.arith.BigComplex;
import com.p6majo.core.cas.arith.BigInteger;
import com.p6majo.core.cas.arith.BigRational;
import com.p6majo.core.cas.extensions.ExtensionManager;
import com.p6majo.core.cas.poly.GenPolynomial;
import com.p6majo.core.cas.poly.GenPolynomialRing;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class AlgebraicNumberRingTest {



    private ExtensionManager em ;
    private GenPolynomialRing<BigRational> fac;

    @Before
    public void setUp() throws Exception {

        //this shows a generic procedure to build up a stepwise extension
        //the focus is restricted to square roots for simplicity


        em = new ExtensionManager(new BigRational(),"a");
        fac=em.getExtensionFactory();
        em.addExtension(fac.parse("7"));
        fac = em.getExtensionFactory();
        System.out.println(fac.toString());
        em.addExtension(fac.parse("a0-1"));
        fac = em.getExtensionFactory();
        em.addExtension(fac.parse("a0+a1"));
        fac = em.getExtensionFactory();
        em.addExtension(fac.parse("a0*a2"));
        System.out.println(em.toString());

    }

    @Test
    public void parse() {
        System.out.println("Now construct algebraic numbers with this extension: ");
        AlgebraicNumberRing<BigRational> anr = new AlgebraicNumberRing<>(em);

        for (int i = 0; i < 10; i++) {
            GenPolynomial<BigRational> pol = fac.random(10);
            System.out.println("The polynomial " + pol.toString() + " is converted into the algebraic number: " + anr.parse(pol.toString()));
        }

        System.out.println("Check the multiplication: ");
        for (int i = 0; i < 10; i++) {
            AlgebraicNumber<BigRational> fac1 = anr.parse(fac.random(2).toString());
            AlgebraicNumber<BigRational> fac2 = anr.parse(fac.random(2).toString());

            System.out.println("["+fac1.toString() + "] * [" + fac2.toString() + "] = " + fac1.mul(fac2).toString());
        }
    }

    @Test
    public void complexCase(){
        em = new ExtensionManager(new BigComplex(),"a");
        fac = em.getExtensionFactory();
        em.addExtension(fac.parse("7"));
        fac = em.getExtensionFactory();
        em.addExtension(fac.parse("a0+3"));
        System.out.println(em.toString());
    }
}