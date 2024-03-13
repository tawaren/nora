package nora.vm.nodes.compare;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.BooleanNode;
import nora.vm.nodes.consts.ByteNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.IntNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "left", type = NoraNode.class)
@NodeChild(value = "right", type = NoraNode.class)
@ReportPolymorphism
@NodeInfo(shortName = ">=")
public abstract class GteNode extends NoraNode {
    abstract NoraNode getLeft();
    abstract NoraNode getRight();

    @Specialization
    protected boolean evalByte(byte left, byte right){
        return left >= right;
    }

    @Specialization
    protected boolean evalInt(int left, int right){
        return left >= right;
    }

    @Specialization
    protected boolean evalLong(long left, long right){
        return left >= right;
    }

    @Megamorphic
    @Specialization(replaces = "evalLong")
    protected boolean evalBigInteger(BigInteger left, BigInteger right){
        return left.compareTo(right) >= 0;
    }

    @Specialization
    public boolean evalString(TruffleString left, TruffleString right,
                              @Cached TruffleString.CompareBytesNode cmpNode
    ) {
        return cmpNode.execute(left, right, TruffleString.Encoding.UTF_16) >= 0;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        var leftRes = getLeft().specialise(frame);
        var rightRes = getRight().specialise(frame);
        if(leftRes instanceof ConstNode && rightRes instanceof ConstNode){
            if(leftRes.getType(frame).info == util.StringTypeInfo && rightRes.getType(frame).info == util.StringTypeInfo ){
                return ConstNode.create(TruffleString.CompareBytesNode.getUncached().execute(
                        leftRes.executeString(null),
                        rightRes.executeString(null),
                        TruffleString.Encoding.UTF_16
                ) >= 0);
            }
            if(leftRes instanceof ByteNode in1 && rightRes instanceof ByteNode in2){
                return ConstNode.create(evalByte(in1.executeByte(null),in2.executeByte(null)));
            }
            if(leftRes instanceof IntNode in1 && rightRes instanceof IntNode in2){
                return ConstNode.create(evalInt(in1.executeInt(null),in2.executeInt(null)));
            }
            try {
                boolean res = evalLong(leftRes.executeLong(null), rightRes.executeLong(null));
                return ConstNode.create(res);
            } catch (ArithmeticException | UnexpectedResultException ex){
                boolean res = evalBigInteger(leftRes.executeBigInteger(null), rightRes.executeBigInteger(null));
                return ConstNode.create(res);
            }
        } else if(leftRes instanceof CachedNode cn1 && rightRes instanceof CachedNode cn2) {
            return new CacheNode(GteNodeGen.create(cn1.liftCache(), cn2.liftCache()));
        } else {
            if(!util.isSimpleType(leftRes.getType(frame).info)){
                //Sadly we do not know how expensive this will be
                //Todo: we could ask schema
                frame.markNonTrivial();
            }
            return GteNodeGen.create(leftRes, rightRes);
        }
    }

    @Override
    public String toString() {
        return "Gte(" +getLeft()+"," + getRight() +")";
    }

    @Override
    public GteNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return GteNodeGen.create(getLeft().cloneUninitialized(),getRight().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).BooleanType;
    }

}
