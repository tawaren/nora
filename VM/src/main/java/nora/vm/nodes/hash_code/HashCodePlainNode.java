package nora.vm.nodes.hash_code;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.ArgNode;
import nora.vm.nodes.equality.EqNode;
import nora.vm.nodes.equality.EqPlainNodeGen;
import nora.vm.nodes.equality.calls.EqPlainCallRootNode;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.equality.data.DataEqRecursionNodeGen;
import nora.vm.nodes.hash_code.calls.HashCodePlainCallRootNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.hash_code.data.DataHashCodeRecursionNodeGen;
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
public abstract class HashCodePlainNode extends Node {

    abstract int getRecursionDepth();

    public abstract int execute(Object left);

    @Specialization
    public int evalByte(byte left){
        return Byte.hashCode(left);
    }

    @Specialization
    public int evalBool(boolean left){
        return Boolean.hashCode(left);
    }

    @Specialization
    public int evalInt(int left){
        return Integer.hashCode(left);
    }

    @Specialization
    public int evalLong(long left){
        return NumHashCode.hashCode(left);
    }

    @Specialization
    public int evalBig(BigInteger left){
        return NumHashCode.hashCode(left);
    }

    @Specialization
    public int evalBoolArray(boolean[] left){
        return Arrays.hashCode(left);
    }

    @Specialization
    public int evalByteArray(byte[] left){
        return Arrays.hashCode(left);
    }

    @Specialization
    public int evalIntArray(int[] left){
        return Arrays.hashCode(left);
    }

    @Specialization
    public int evalLongArray(long[] left){
        return Arrays.hashCode(left);
    }

    @Specialization(guards = "!recursionLimitReached()")
    public int evalObjectArray(Object[] left,
                                 @Cached(value = "createPlainHashCodeNode()", uncached = "createUncachedPlainHashCodeNode()")
                                 HashCodePlainNode inner
    ){
        int result = 1;
        for (Object element : left) result = 31 * result + inner.execute(element);
        return result;
    }


    @Specialization(guards = "recursionLimitReached()")
    public int evalObjectArrayCall(Object[] left,
                                   @Cached(value = "createPlainHashCodeCallNode()", uncached = "createNullCall()")
                                   DirectCallNode inner
    ){
        assert inner != null;
        int result = 1;
        for (Object element : left) result = 31 * result + ((int)inner.call(element));
        return result;
    }

    @Idempotent
    protected boolean recursionLimitReached() {
        if(this == HashCodePlainNodeGen.getUncached()) return false;
        return getRecursionDepth() >= HashCodeNode.MAX_RECURSION_DEPTH;
    }

    @Specialization
    public int evalString(TruffleString left, @Cached TruffleString.HashCodeNode hashNode) {
        return hashNode.execute(left, TruffleString.Encoding.UTF_16);
    }

    @Specialization
    public int evalId(IdentityObject left){
        return System.identityHashCode(left);
    }

    @Specialization
    public int evalData(NoraData left,
                            @Cached(value = "createDataHashCodeNode()", uncached = "createUncachedDataHashCodeNode()")
                            DataHashCodeNode hashNode) {
        return hashNode.executeHashCode(left);
    }

    @TruffleBoundary
    @Specialization
    public int evalOthers(Object left){
        return left.hashCode();
    }

    @NeverDefault
    protected HashCodePlainNode createPlainHashCodeNode(){
        return HashCodePlainNodeGen.create(getRecursionDepth()+1);
    }

    protected DirectCallNode createNullCall(){
        return null;
    }

    @NeverDefault
    protected DirectCallNode createPlainHashCodeCallNode(){
        var node = HashCodePlainNodeGen.create(0);
        var root = new HashCodePlainCallRootNode(new ArgNode(0),node);
        return DirectCallNode.create(root.getCallTarget());
    }

    @NeverDefault
    protected DataHashCodeNode createDataHashCodeNode(){
        return DataHashCodeRecursionNodeGen.create(getRecursionDepth());
    }

    @NeverDefault
    protected DataHashCodeNode createUncachedDataHashCodeNode(){
        return DataHashCodeRecursionNodeGen.getUncached();
    }

    @NeverDefault
    protected HashCodePlainNode createUncachedPlainHashCodeNode(){
        return HashCodePlainNodeGen.getUncached();
    }


    @Override
    public String toString() {
        return "HashCode";
    }

    public HashCodePlainNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return HashCodePlainNodeGen.create(getRecursionDepth());
    }

}
