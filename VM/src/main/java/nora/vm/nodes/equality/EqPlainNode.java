package nora.vm.nodes.equality;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.ArgNode;
import nora.vm.nodes.equality.calls.EqPlainCallRootNode;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.equality.data.DataEqRecursionNodeGen;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.Type;

@NodeInfo(shortName = "==")
@NodeField(name = "recursionDepth", type = int.class)
@ImportStatic(EqNode.class)
@GenerateInline(false)
@GenerateUncached
@ReportPolymorphism
public abstract class EqPlainNode extends Node {

    abstract int getRecursionDepth();

    public abstract boolean execute(Object left, Object right);

    @Specialization
    public boolean evalByte(byte left, byte right){
        return left == right;
    }

    @Specialization
    public boolean evalBool(boolean left, boolean right){
        return left == right;
    }

    @Specialization
    public boolean evalInt(int left, int right){
        return left == right;
    }

    @Specialization
    public boolean evalLong(long left, long right){
        return left == right;
    }

    @Specialization
    public boolean evalBoolArray(boolean[] left, boolean[] right){
        if(left == right) return true;
        if(left.length != right.length) return false;
        for(int i = 0; i < left.length; i++ ){
            if(left[i] != right[i]) return false;
        }
        return true;
    }

    @Specialization
    public boolean evalByteArray(byte[] left, byte[] right){
        if(left == right) return true;
        if(left.length != right.length) return false;
        for(int i = 0; i < left.length; i++ ){
            if(left[i] != right[i]) return false;
        }
        return true;
    }

    @Specialization
    public boolean evalIntArray(int[] left, int[] right){
        if(left == right) return true;
        if(left.length != right.length) return false;
        for(int i = 0; i < left.length; i++ ){
            if(left[i] != right[i]) return false;
        }
        return true;
    }

    @Specialization
    public boolean evalLongArray(long[] left, long[] right){
        if(left == right) return true;
        if(left.length != right.length) return false;
        for(int i = 0; i < left.length; i++ ){
            if(left[i] != right[i]) return false;
        }
        return true;
    }

    @Specialization(guards = "!recursionLimitReached()")
    public boolean evalObjectArray(Object[] left, Object[] right,
                                 @Cached(value = "createPlainEqNode()", uncached = "createUncachedPlainEqNode()")
                                 EqPlainNode inner
    ){
        if(left == right) return true;
        if(left.length != right.length) return false;
        for(int i = 0; i < left.length; i++ ){
            if(inner.execute(left[i],right[i])) return false;
        }
        return true;
    }


    @Specialization(guards = "recursionLimitReached()")
    public boolean evalObjectArrayCall(Object[] left, Object[] right,
                                   @Cached(value = "createPlainEqCallNode()", uncached = "createNullCall()")
                                   DirectCallNode inner
    ){
        assert inner != null;
        if(left == right) return true;
        if(left.length != right.length) return false;
        for(int i = 0; i < left.length; i++ ){
            //will not be called in inner is null
            if((Boolean) inner.call(left[i],right[i])) return false;
        }
        return true;
    }

    @Idempotent
    protected boolean recursionLimitReached() {
        if(this == EqPlainNodeGen.getUncached()) return false;
        return getRecursionDepth() >= EqNode.MAX_RECURSION_DEPTH;
    }

    @Specialization
    public boolean evalString(TruffleString left, TruffleString right, @Cached TruffleString.EqualNode eqNode) {
        return eqNode.execute(left, right, TruffleString.Encoding.UTF_16);
    }

    @Specialization
    public boolean evalId(IdentityObject left, IdentityObject right){
        return left == right;
    }

    @Specialization
    public boolean evalData(NoraData left, NoraData right,
                            @Cached(value = "createDataEqNode()", uncached = "createUncachedDataEqNode()")
                            DataEqNode eqNode) {
        return eqNode.executeDataEq(left,right);
    }

    @Specialization
    public boolean evalType(Type left, Type right) {
        return left.sameType(right);
    }

    @TruffleBoundary
    @Specialization
    public boolean evalOthers(Object left, Object right){
        return left.equals(right);
    }

    @NeverDefault
    protected EqPlainNode createPlainEqNode(){
        return EqPlainNodeGen.create(getRecursionDepth()+1);
    }

    protected DirectCallNode createNullCall(){
        return null;
    }

    @NeverDefault
    protected DirectCallNode createPlainEqCallNode(){
        var node = EqPlainNodeGen.create(0);
        var root = new EqPlainCallRootNode(new ArgNode(0),new ArgNode(1),node);
        return DirectCallNode.create(root.getCallTarget());
    }

    @NeverDefault
    protected DataEqNode createDataEqNode(){
        return DataEqRecursionNodeGen.create(getRecursionDepth());
    }

    @NeverDefault
    protected DataEqNode createUncachedDataEqNode(){
        return DataEqRecursionNodeGen.getUncached();
    }

    @NeverDefault
    protected EqPlainNode createUncachedPlainEqNode(){
        return EqPlainNodeGen.getUncached();
    }


    @Override
    public String toString() {
        return "Eq";
    }

    public EqPlainNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return EqPlainNodeGen.create(getRecursionDepth());
    }

}
