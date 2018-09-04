package trc.com.p6majo.core.cas.extensions;

import com.p6majo.core.cas.algebraicNumbers.AlgebraicNumber;
import com.p6majo.core.cas.algebraicNumbers.AlgebraicNumberRing;
import com.p6majo.core.cas.arith.BigInteger;
import com.p6majo.core.cas.arith.BigRational;
import com.p6majo.core.cas.exceptions.CoefficientFactoryMismatchException;
import com.p6majo.core.cas.extensions.ExtensionManager;
import com.p6majo.core.cas.extensions.SquareRoot;
import com.p6majo.core.cas.poly.GenPolynomial;
import com.p6majo.core.cas.poly.GenPolynomialRing;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class ExtensionManagerTest {


    private GenPolynomialRing polFac1;
    private GenPolynomialRing polFac2;
    private GenPolynomialRing polFac3;
    private GenPolynomial<BigRational> pol1;
    private GenPolynomial<BigRational> pol2;
    private GenPolynomial<BigRational> pol3;
    private GenPolynomial<BigRational> pol4;

    private ExtensionManager em ;

    @Before
    public void setUp() throws Exception {
        polFac1 = new GenPolynomialRing(new BigRational(1),3,new String[]{"a1","a2","a3"});
        polFac2 = new GenPolynomialRing(new BigRational(1),2,new String[]{"a4","a1"});
        polFac3 = new GenPolynomialRing(new BigInteger(1),2,new String[]{"a1","a2"});

        pol1 = polFac1.parse("3*a1*a2*a3");
        pol2 = polFac2.parse("1/2*a1*a4^2");
        pol3 = polFac1.parse("a2");
        pol4 = polFac3.random(10);

        em = new ExtensionManager(new BigRational(),"a");

    }


    @Test
    public void getMinimalExtension() {
         System.out.println(em.getMinimalExtension(pol1, pol2, pol3));

        try {
            em.getMinimalExtension(pol1, pol2, pol3, pol4);
        }
        catch(CoefficientFactoryMismatchException ex){
            System.out.println(ex.getMessage());
        }
    }

    @Test
    public void addExtension() {
        em.addExtension(new BigRational(7));
        System.out.println("Factory after the first extension: " + em.getExtensionFactory().toString());
        GenPolynomialRing fac = em.getExtensionFactory();
        em.addExtension(fac.parse("a0+3"));
        System.out.println("Factory after the second extension: " + em.getExtensionFactory().toString());
        System.out.println(em.toString());
        List<SquareRoot> roots = em.getRoots();

        System.out.println("List the moduli as they are constructed:\n");
        for (SquareRoot root : roots) {
            System.out.println(root.getModulus());
        }

        System.out.println("Construct algebraic numbers from the given extensions: ");
        AlgebraicNumberRing<BigRational> anr = new AlgebraicNumberRing<>(em);

        AlgebraicNumber<BigRational> an = anr.parse("a0^3-a1^7");
        System.out.println(an.toString());
    }

    @Test
    public void leadingBaseCoefficientWrtRoot() {
        em.addExtension(new BigRational(7));
        System.out.println("Factory after the first extension: " + em.getExtensionFactory().toString());
        GenPolynomialRing fac = em.getExtensionFactory();
        em.addExtension(fac.parse("a0+3"));
        System.out.println("Factory after the second extension: " + em.getExtensionFactory().toString());
        System.out.println(em.toString());
        List<SquareRoot> roots = em.getRoots();


        GenPolynomialRing<BigRational> factory= em.getExtensionFactory();
        GenPolynomial<BigRational> testPol = factory.parse("a0^3-a1^7");
        System.out.println(em.leadingBaseCoefficientWrtRoot(testPol,0));
        System.out.println(em.leadingBaseCoefficientWrtRoot(testPol,1));

        System.out.println(em.leadingExponentWrtRoot(testPol,0).toString());
        System.out.println(em.leadingExponentWrtRoot(testPol,1).toString());
    }
}