package nora.vm.nodes.string.to_string;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.ArgNode;
import nora.vm.nodes.hash_code.HashCodeNode;
import nora.vm.nodes.hash_code.HashCodePlainNode;
import nora.vm.nodes.hash_code.HashCodePlainNodeGen;
import nora.vm.nodes.hash_code.NumHashCode;
import nora.vm.nodes.hash_code.calls.HashCodePlainCallRootNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.hash_code.data.DataHashCodeRecursionNodeGen;
import nora.vm.nodes.string.to_string.calls.ToStringPlainCallRootNode;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.nodes.string.to_string.data.DataToStringRecursionNodeGen;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;

import java.math.BigInteger;
import java.util.Arrays;

@NodeInfo(shortName = "==")
@NodeField(name = "recursionDepth", type = int.class)
@ImportStatic(HashCodeNode.class)
@GenerateInline(false)
@GenerateUncached
@ReportPolymorphism
public abstract class ToStringPlainNode extends Node {

    abstract int getRecursionDepth();

    public abstract void execute(TruffleStringBuilder builder, Object val);

    @Specialization
    public void evalByte(TruffleStringBuilder builder, byte val, @Cached.Shared("appendByte") @Cached TruffleStringBuilder.AppendByteNode append){
        append.execute(builder, val);
    }

    @Specialization
    public void evalBool(TruffleStringBuilder builder, boolean val, @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append){
        //Todo: have them as static consts and use TruffleString append
        if(val) append.execute(builder, "true");
        else append.execute(builder, "false");
    }
    @Specialization
    public void evalInt(TruffleStringBuilder builder, int val, @Cached.Shared("appendInt") @Cached TruffleStringBuilder.AppendIntNumberNode append){
        append.execute(builder, val);
    }

    @Specialization
    public void evalLong(TruffleStringBuilder builder, long val, @Cached.Shared("appendLong") @Cached TruffleStringBuilder.AppendLongNumberNode append){
        append.execute(builder, val);
    }

    //Todo: Some Sharing of the Java Appender
    @Specialization
    public void evalBig(TruffleStringBuilder builder, BigInteger val, @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append){
        append.execute(builder, val.toString());
    }

    //Todo: Some Sharing of the Java Appender
    @Specialization
    public void evalBoolArray(TruffleStringBuilder builder, boolean[] val,
                              @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append){
        append.execute(builder,"[");
        for(int i = 0; i < val.length; i++){
            evalBool(builder,val[i], append);
            if(i != 0)append.execute(builder,", ");
        }
        append.execute(builder, "]");
    }

    @Specialization
    public void evalByteArray(TruffleStringBuilder builder, byte[] val,
                              @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append,
                              @Cached.Shared("appendByte") @Cached TruffleStringBuilder.AppendByteNode appendByte){
        append.execute(builder,"[");
        for(int i = 0; i < val.length; i++){
            evalByte(builder,val[i], appendByte);
            if(i != 0)append.execute(builder,", ");
        }
        append.execute(builder, "]");
    }

    @Specialization
    public void evalIntArray(TruffleStringBuilder builder, int[] val,
                             @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append,
                             @Cached.Shared("appendInt") @Cached TruffleStringBuilder.AppendIntNumberNode appendInt){
        append.execute(builder,"[");
        for(int i = 0; i < val.length; i++){
            evalInt(builder,val[i], appendInt);
            if(i != 0)append.execute(builder,", ");
        }
        append.execute(builder, "]");
    }

    @Specialization
    public void evalLongArray(TruffleStringBuilder builder, long[] val,
                              @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append,
                              @Cached.Shared("appendLong") @Cached TruffleStringBuilder.AppendLongNumberNode appendLong){
        append.execute(builder,"[");
        for(int i = 0; i < val.length; i++){
            evalLong(builder,val[i], appendLong);
            if(i != 0)append.execute(builder,", ");
        }
        append.execute(builder, "]");
    }

    @Specialization(guards = "!recursionLimitReached()")
    public void evalObjectArray(TruffleStringBuilder builder, Object[] val,
                                @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append,
                                @Cached(value = "createPlainToStringNode()", uncached = "createUncachedPlainToStringNode()")
                                    ToStringPlainNode inner){
        append.execute(builder,"[");
        for(int i = 0; i < val.length; i++){
            inner.execute(builder,val[i]);
            if(i != 0)append.execute(builder,", ");
        }
        append.execute(builder, "]");
    }


    @Specialization(guards = "recursionLimitReached()")
    public void evalObjectArrayCall(TruffleStringBuilder builder, Object[] val,
                                    @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append,
                                    @Cached(value = "createPlainToStringCallNode()", uncached = "createNullCall()")
                                        DirectCallNode inner){
        assert inner != null;
        append.execute(builder,"[");
        for(int i = 0; i < val.length; i++){
            inner.call(builder,val[i]);
            if(i != 0)append.execute(builder,", ");
        }
        append.execute(builder, "]");
    }

    @Specialization
    public void evalString(TruffleStringBuilder builder, TruffleString val, @Cached TruffleStringBuilder.AppendStringNode append) {
        append.execute(builder, val);
    }

    @Specialization
    public void evalData(TruffleStringBuilder builder, NoraData left,
                        @Cached(value = "createDataToStringNode()", uncached = "createUncachedDataToStringNode()")
                        DataToStringNode toString) {
        toString.executeToString(builder, left);
    }


    @TruffleBoundary
    @Specialization
    public void evalOthers(TruffleStringBuilder builder, Object val, @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node append){
        append.execute(builder, val.toString());
    }

    @Idempotent
    protected boolean recursionLimitReached() {
        if(this == ToStringPlainNodeGen.getUncached()) return false;
        return getRecursionDepth() >= ToStringNode.MAX_RECURSION_DEPTH;
    }

    @NeverDefault
    protected ToStringPlainNode createPlainToStringNode(){
        return ToStringPlainNodeGen.create(getRecursionDepth()+1);
    }

    protected DirectCallNode createNullCall(){
        return null;
    }

    @NeverDefault
    protected DirectCallNode createPlainToStringCallNode(){
        var node = ToStringPlainNodeGen.create(0);
        var root = new ToStringPlainCallRootNode(new ArgNode(0),new ArgNode(1),node);
        return DirectCallNode.create(root.getCallTarget());
    }

    @NeverDefault
    protected DataToStringNode createDataToStringNode(){
        return DataToStringRecursionNodeGen.create(getRecursionDepth());
    }

    @NeverDefault
    protected DataToStringNode createUncachedDataToStringNode(){
        return DataToStringRecursionNodeGen.getUncached();
    }

    @NeverDefault
    protected ToStringPlainNode createUncachedPlainToStringNode(){
        return ToStringPlainNodeGen.getUncached();
    }


    @Override
    public String toString() {
        return "HashCode";
    }

    public ToStringPlainNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return ToStringPlainNodeGen.create(getRecursionDepth());
    }

}
