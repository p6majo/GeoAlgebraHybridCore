package trc.com.p6majo.core.cas.poly;

import com.p6majo.core.cas.arith.BigRational;
import com.p6majo.core.cas.poly.GenPolynomialRing;
import org.junit.Test;

import static org.junit.Assert.*;

public class GenPolynomialTest {

    @Test
    public void toStringTest() {
        GenPolynomialRing<BigRational> polFac = new GenPolynomialRing<>(new BigRational(1),3);
        polFac.setVars(new String[]{"x","y","z"});
        System.out.println(polFac.toString());

        System.out.println(polFac.parse("3*x*y*z"));
    }
}