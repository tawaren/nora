package nora.vm.nodes.interop;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import nora.vm.truffle.NoraLanguage;

import java.math.BigInteger;


@ExportLibrary(InteropLibrary.class)
public class BigWrapper implements TruffleObject {
    public final BigInteger bigInt;

    public BigWrapper(BigInteger bigInt) {
        this.bigInt = bigInt;
    }

    @ExportMessage
    public final boolean isMetaInstance(Object instance){
        return false;
    }

    @ExportMessage
    public final boolean isMetaObject(){
        return false;
    }

    @ExportMessage
    public final Object getMetaQualifiedName() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    final Object getMetaSimpleName() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }


    @ExportMessage
    public final String toDisplayString(boolean allowSideEffects){
        return bigInt.toString();
    }

    @ExportMessage
    public final boolean hasLanguage(){
        return true;
    }

    @ExportMessage
    public final Class<? extends TruffleLanguage<?>> getLanguage(){
        return NoraLanguage.class;
    }

    @ExportMessage
    public final boolean isNumber(){
        return true;
    }

    @ExportMessage
    public final boolean fitsInByte() {
        return bigInt.compareTo(BigInteger.valueOf(Byte.MAX_VALUE)) <= 0 &&
                bigInt.compareTo(BigInteger.valueOf(Byte.MIN_VALUE)) >= 0;
    }

    @ExportMessage
    public boolean fitsInShort() {
        return bigInt.compareTo(BigInteger.valueOf(Short.MAX_VALUE)) <= 0 &&
                bigInt.compareTo(BigInteger.valueOf(Short.MIN_VALUE)) >= 0;
    }

    @ExportMessage
    public boolean fitsInInt() {
        return bigInt.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0 &&
                bigInt.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0;
    }

    @ExportMessage
    public boolean fitsInLong() {
        return bigInt.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0 &&
                bigInt.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0;
    }

    @ExportMessage
    public boolean fitsInBigInteger() {
        return true;
    }

    @ExportMessage
    public boolean fitsInFloat() {
        return false;
    }

    @ExportMessage
    public boolean fitsInDouble() {
        return false;
    }

    @ExportMessage
    public byte asByte() throws UnsupportedMessageException {
        try{
            return bigInt.byteValueExact();
        }catch (ArithmeticException ae){
            throw UnsupportedMessageException.create(ae);
        }
    }

    @ExportMessage
    public short asShort() throws UnsupportedMessageException {
        try{
            return bigInt.shortValueExact();
        }catch (ArithmeticException ae){
            throw UnsupportedMessageException.create(ae);
        }
    }

    @ExportMessage
    public int asInt() throws UnsupportedMessageException {
        try{
            return bigInt.intValueExact();
        }catch (ArithmeticException ae){
            throw UnsupportedMessageException.create(ae);
        }
    }

    @ExportMessage
    public long asLong() throws UnsupportedMessageException {
        try{
            return bigInt.longValueExact();
        }catch (ArithmeticException ae){
            throw UnsupportedMessageException.create(ae);
        }
    }

    @ExportMessage
    public BigInteger asBigInteger() throws UnsupportedMessageException {
        return bigInt;
    }

    @ExportMessage
    public float asFloat() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }

    @ExportMessage
    public double asDouble() throws UnsupportedMessageException {
        throw UnsupportedMessageException.create();
    }
}
