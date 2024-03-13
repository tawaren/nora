package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.utils.ConcreteTypeOfUtilNode;
import nora.vm.nodes.utils.ConcreteTypeOfUtilNodeGen;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

@NodeChild(value = "src", type = NoraNode.class)
public abstract class ConcreteTypeOfNode extends NoraNode {
    @Child private ConcreteTypeOfUtilNode typeUtilNode = ConcreteTypeOfUtilNodeGen.create();
    abstract NoraNode getSrc();

    @Specialization
    public int concreteTypeOf(byte i){
        return typeUtilNode.executeIndexTypeOf(i);
    }

    @Specialization
    public int concreteTypeOf(int i){
        return typeUtilNode.executeIndexTypeOf(i);
    }

    @Specialization
    public int concreteTypeOf(long l){
        return typeUtilNode.executeIndexTypeOf(l);
    }

    @Specialization
    public int concreteTypeOf(boolean b){
        return typeUtilNode.executeIndexTypeOf(b);
    }

    @Specialization
    public int concreteTypeOf(Object obj){
        return typeUtilNode.executeIndexTypeOf(obj);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var newSrc = getSrc().specialise(frame);
        if(newSrc instanceof ConstNode){
            var trg = newSrc.execute(null);
            return ConstNode.create(typeUtilNode.getGenericType(trg));
        } else if(newSrc instanceof CachedNode cn) {
            return new CacheNode(ConcreteTypeOfNodeGen.create(cn.liftCache()));
        } else {
            return ConcreteTypeOfNodeGen.create(newSrc);
        }
    }

    @Override
    public String toString() {
        return "typeIndexOf("+getSrc()+")";
    }

    @Override
    public ConcreteTypeOfNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return ConcreteTypeOfNodeGen.create(getSrc().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).IntType;
    }

    @Override
    public int complexity() {
        return 2;
    }
}
