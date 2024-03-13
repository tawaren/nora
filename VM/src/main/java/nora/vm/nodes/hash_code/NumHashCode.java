package nora.vm.nodes.hash_code;

import java.math.BigInteger;

public class NumHashCode {
    public static int hashCode(long l){
        return Long.hashCode(l);
    }

    public static int hashCode(BigInteger b){
        //Todo: Find a better way
        try {
            return hashCode(b.longValueExact());
        } catch (ArithmeticException e){
            return b.hashCode();
        }
    }
}
