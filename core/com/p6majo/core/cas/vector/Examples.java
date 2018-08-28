/*
 * $Id$
 */

package com.p6majo.core.cas.vector;


import com.p6majo.core.cas.arith.BigInteger;
import com.p6majo.core.cas.arith.BigRational;

/**
 * Examples for basic linear algebra.
 * @author Heinz Kredel
 */

public class Examples {

    /**
     * main.
     */
    public static void main (String[] args) {
        example1();
        example2();
        // ComputerThreads.terminate();
    }


    /**
     * example1.
     */
    public static void example1() {
        System.out.println("\n\n example 1");

        BigInteger cfac;
        GenMatrixRing<BigInteger> mfac;

        cfac = new BigInteger();
        System.out.println("cfac = " + cfac);

        mfac = new GenMatrixRing<BigInteger>( cfac, 5, 5 );
        System.out.println("mfac = " + mfac);

        GenMatrix<BigInteger> m;
        m = mfac.random(3,0.4f);
        System.out.println("\nm = " + m);

        m = m.multiply(m);
        System.out.println("\nm = " + m);
    }


    /**
     * example2.
     */
    public static void example2() {
        System.out.println("\n\n example 2");

        BigRational cfac;
        GenMatrixRing<BigRational> mfac;

        cfac = new BigRational();
        System.out.println("cfac = " + cfac);

        mfac = new GenMatrixRing<BigRational>( cfac, 5, 5 );
        System.out.println("mfac = " + mfac);

        GenMatrix<BigRational> m;
        m = mfac.random(3,0.4f);
        System.out.println("\nm = " + m);

        m = m.multiply(m);
        System.out.println("\nm = " + m);
    }

}
