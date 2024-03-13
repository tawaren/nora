package nora.vm.nodes.bitops;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.NodeInfo;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.arith.SubNodeGen;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.*;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;


@NodeChild(value = "val", type = NoraNode.class)
@NodeChild(value = "amount", type = NoraNode.class)
@ReportPolymorphism
@NodeInfo(shortName = "<<")
public abstract class LeftShiftNode extends NoraNode {
    abstract NoraNode getVal();
    abstract NoraNode getAmount();

    @Specialization
    public byte evalByte(byte val, int amount) {
        return (byte) (val << amount);
    }

    @Specialization
    public int evalInt(int val, int amount) {
        return val << amount;
    }

    @Specialization(rewriteOn = ArithmeticException.class)
    public long evalLong(long val, int amount) throws ArithmeticException {
        var res = val << amount;
        if((val >> amount) != res) throw new ArithmeticException();
        return res;
    }

    @Specialization
    public BigInteger evalBig(BigInteger val, int amount) {
        return val.shiftRight(amount);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var valSpec = getVal().specialise(frame);
        var amountSpec = getAmount().specialise(frame);
        if(valSpec instanceof ConstNode & amountSpec instanceof ConstNode){
            var valRes = valSpec.execute(null);
            var amountRes = amountSpec.executeInt(null);
            if(valRes instanceof Byte lb){
                return ConstNode.create(evalByte(lb, amountRes));
            }
            if(valRes instanceof Integer lb){
                return ConstNode.create(evalInt(lb, amountRes));
            }
            if(valRes instanceof Long lb){
                return ConstNode.create(evalLong(lb, amountRes));
            }
            if(valRes instanceof BigInteger a){
                return ConstNode.create(evalBig(a, amountRes));
            }
        } else if(valSpec instanceof CachedNode cn1 && amountSpec instanceof CachedNode cn2) {
            return new CacheNode(LeftShiftNodeGen.create(cn1.liftCache(), cn2.liftCache()));
        }
        return LeftShiftNodeGen.create(valSpec, amountSpec);
    }

    @Override
    public String toString() {
        return "LeftShift(" +getVal()+"," + getAmount() +")";
    }

    @Override
    public LeftShiftNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return LeftShiftNodeGen.create(getVal().cloneUninitialized(),getAmount().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return getVal().getType(frame);
    }
}
