/*
 * $Id$
 */

package com.p6majo.core.cas.arith;


/**
 * Interface with method to get a BigRational (approximation).
 * @author Heinz Kredel
 */

public interface Rational {
    /**
     * Return a BigRational approximation of this Element.
     * @return a BigRational approximation of this.
     */
    public BigRational getRational();

}
