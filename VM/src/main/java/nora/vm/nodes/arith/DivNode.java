package nora.vm.nodes.arith;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.*;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "left", type = NoraNode.class)
@NodeChild(value = "right", type = NoraNode.class)
@ReportPolymorphism
@NodeInfo(shortName = "/")
public abstract class DivNode extends NoraNode {
    abstract NoraNode getLeft();
    abstract NoraNode getRight();

    @Specialization
    protected byte evalByte(byte left, byte right){
        return (byte)(left / right);
    }

    @Specialization
    protected int evalInt(int left, int right){
        return left / right;
    }

    @Specialization
    protected long evalLong(long left, long right){
        return left / right;
    }

    @Megamorphic
    @TruffleBoundary
    @Specialization(replaces = "evalLong")
    protected BigInteger evalBigInteger(BigInteger left, BigInteger right) {
        return left.divide(right);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var leftRes = getLeft().specialise(frame);
        var rightRes = getRight().specialise(frame);
        if(leftRes instanceof ConstNode && rightRes instanceof ConstNode){
            if(leftRes instanceof IntNode in1 && rightRes instanceof IntNode in2){
                return ConstNode.create(evalInt(in1.executeInt(null),in2.executeInt(null)));
            }
            if(leftRes instanceof ByteNode in1 && rightRes instanceof ByteNode in2){
                return ConstNode.create(evalByte(in1.executeByte(null),in2.executeByte(null)));
            }
            try {
                long res = evalLong(leftRes.executeLong(null), rightRes.executeLong(null));
                return ConstNode.create(res);
            } catch (UnexpectedResultException ex){
                BigInteger res = evalBigInteger(leftRes.executeBigInteger(null), rightRes.executeBigInteger(null));
                return ConstNode.create(res);
            }
        } else if(leftRes instanceof CachedNode cn1 && rightRes instanceof CachedNode cn2) {
            return new CacheNode(DivNodeGen.create(cn1.liftCache(), cn2.liftCache()));
        } else {
            return DivNodeGen.create(leftRes, rightRes);
        }
    }

    @Override
    public String toString() {
        return "Div(" +getLeft()+"," + getRight() +")";
    }

    @Override
    public DivNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DivNodeGen.create(getLeft().cloneUninitialized(),getRight().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        var left = getLeft().getType(frame);
        if(left != null) return left;
        return getRight().getType(frame);
    }

}
