package nora.vm.nodes.bitops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import nora.vm.nodes.CreateNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.*;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "op", type = NoraNode.class)
@ReportPolymorphism
@NodeInfo(shortName = "~")
public abstract class NotNode extends NoraNode {
    abstract NoraNode getOp();

    @Specialization
    public boolean evalBool(boolean op) {
        return !op;
    }

    @Specialization
    public byte evalByte(byte op) {
        return (byte) ~op;
    }

    @Specialization
    public int evalInt(int op) {
        return ~op;
    }

    @Specialization
    public long evalLong(long op) {
        return ~op;
    }

    @Specialization
    public BigInteger evalBig(BigInteger op) {
        return op.not();
    }


    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var opSpec = getOp().specialise(frame);
        if(opSpec instanceof ConstNode){
            var opRes = opSpec.execute(null);
            if(opRes instanceof Boolean lb){
                return ConstNode.create(evalBool(lb));
            }
            if(opRes instanceof Byte lb){
                return ConstNode.create(evalByte(lb));
            }
            if(opRes instanceof Integer lb){
                return ConstNode.create(evalInt(lb));
            }
            if(opRes instanceof Long lb){
                return ConstNode.create(evalLong(lb));
            }
            if(opRes instanceof BigInteger a){
                return ConstNode.create(evalBig(a));
            }
        } else if(opSpec instanceof CachedNode cn1) {
            return new CacheNode(NotNodeGen.create(cn1.liftCache()));
        }
        return NotNodeGen.create(opSpec);
    }

    @Override
    public String toString() {
        return "Not("+getOp()+")";
    }

    @Override
    public NotNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return NotNodeGen.create(getOp().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return getOp().getType(frame);
    }
}
