package nora.vm.nodes.bitops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
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
@NodeInfo(shortName = "^")
public abstract class XorNode extends NoraNode {
    abstract NoraNode getLeft();
    abstract NoraNode getRight();

    @Specialization
    public boolean evalBool(boolean left, boolean right) {
        return left ^ right;
    }

    @Specialization
    public byte evalByte(byte left, byte right) {
        return (byte) (left ^ right);
    }

    @Specialization
    public int evalInt(int left, int right) {
        return left ^ right;
    }

    @Specialization
    public long evalLong(long left, long right) {
        return left ^ right;
    }

    @Specialization
    public BigInteger evalBig(BigInteger left, BigInteger right) {
        return left.xor(right);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var leftSpec = getLeft().specialise(frame);
        var rightSpec = getRight().specialise(frame);
        if (leftSpec instanceof ConstNode & rightSpec instanceof ConstNode) {
            var leftRes = leftSpec.execute(null);
            var rightRes = rightSpec.execute(null);
            if (leftRes instanceof Boolean lb && rightRes instanceof Boolean rb) {
                return ConstNode.create(evalBool(lb, rb));
            }
            if(leftRes instanceof Byte lb && rightRes instanceof Byte rb){
                return ConstNode.create(evalByte(lb, rb));
            }
            if (leftRes instanceof Integer lb && rightRes instanceof Integer rb) {
                return ConstNode.create(evalInt(lb, rb));
            }
            if (leftRes instanceof Long lb && rightRes instanceof Long rb) {
                return ConstNode.create(evalLong(lb, rb));
            }
            if (leftRes instanceof Long l) leftRes = BigInteger.valueOf(l);
            if (rightRes instanceof Long l) rightRes = BigInteger.valueOf(l);
            if (leftRes instanceof BigInteger a && rightRes instanceof BigInteger b) {
                return ConstNode.create(evalBig(a, b));
            }
        } else if(leftSpec instanceof CachedNode cn1 && rightSpec instanceof CachedNode cn2) {
            return new CacheNode(XorNodeGen.create(cn1.liftCache(), cn2.liftCache()));
        }
        return XorNodeGen.create(leftSpec, rightSpec);
    }

    @Override
    public String toString() {
        return "Xor(" +getLeft()+"," + getRight() +")";
    }

    @Override
    public XorNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return XorNodeGen.create(getLeft().cloneUninitialized(),getRight().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        var left = getLeft().getType(frame);
        if(left != null) return left;
        return getRight().getType(frame);
    }
}
