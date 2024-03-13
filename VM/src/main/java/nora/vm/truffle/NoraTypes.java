package nora.vm.truffle;


import com.oracle.truffle.api.dsl.ImplicitCast;
import com.oracle.truffle.api.dsl.TypeSystem;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.method.Function;
import nora.vm.runtime.data.NoraData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.TypeInfo;

import java.math.BigInteger;

@TypeSystem({boolean.class, byte.class,  int.class, long.class, BigInteger.class, TruffleString.class, NoraData.class, Function.class})
public abstract class NoraTypes {

    @ImplicitCast
    public static int castByteToInt(byte value) {
        return value;
    }

    @ImplicitCast
    public static long castByteToLong(byte value) {
        return value;
    }

    @ImplicitCast
    public static BigInteger castByteToBig(byte value) {
        return BigInteger.valueOf(value);
    }

    @ImplicitCast
    public static long castIntToLong(int value) {
        return value;
    }

    @ImplicitCast
    public static BigInteger castIntToBig(int value) {
        return BigInteger.valueOf(value);
    }


    @ImplicitCast
    public static BigInteger castLongToBig(long value) {
        return BigInteger.valueOf(value);
    }
}
