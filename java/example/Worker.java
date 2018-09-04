package example;

import com.p6majo.core.cas.arith.BigRational;
import com.p6majo.core.cas.structure.RingElem;

public class Worker {
    public String getData(){

        RingElem<BigRational> first = new BigRational(3,5);

        RingElem<BigRational> second = first.power(60);



        return second.toString();
    }
}
