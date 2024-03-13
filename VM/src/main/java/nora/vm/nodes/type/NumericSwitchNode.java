package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "inner", type = NoraNode.class)
public abstract class NumericSwitchNode extends SwitchNode {

    abstract NoraNode getInner();

    @Specialization
    long switchLong(long inner){
        return inner;
    }

    @Specialization(replaces = "switchLong")
    BigInteger switchBig(BigInteger big){
        return big;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return NumericSwitchNodeGen.create(getInner().cloneUninitialized());
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var newInner = getInner().specialise(frame);
        if(newInner instanceof ConstNode) return newInner;
        if(newInner instanceof CachedNode cn) return new CacheNode(NumericSwitchNodeGen.create(cn.liftCache()));
        return NumericSwitchNodeGen.create(newInner);
    }

    @Override
    public String toString() {
        return getInner().toString();
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).NumType;
    }

}
