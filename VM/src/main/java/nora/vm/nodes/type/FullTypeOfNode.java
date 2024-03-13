package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.utils.FullTypeOfUtilNode;
import nora.vm.nodes.utils.FullTypeOfUtilNodeGen;
import nora.vm.nodes.utils.TypeOfUtilNode;
import nora.vm.nodes.utils.TypeOfUtilNodeGen;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

@NodeChild(value = "src", type = NoraNode.class)
@ReportPolymorphism
public abstract class FullTypeOfNode extends NoraNode {
    @Child private FullTypeOfUtilNode typeUtilNode = FullTypeOfUtilNodeGen.create();
    abstract NoraNode getSrc();

    @Specialization
    public Type typeOf(byte i){
        return typeUtilNode.executeTypeOf(i);
    }

    @Specialization
    public Type typeOf(int i){
        return typeUtilNode.executeTypeOf(i);
    }

    @Specialization
    public Type typeOf(long l){
        return typeUtilNode.executeTypeOf(l);
    }

    @Specialization
    public Type typeOf(boolean b){
        return typeUtilNode.executeTypeOf(b);
    }

    @Specialization
    public Type typeOf(Object obj){
        return typeUtilNode.executeTypeOf(obj);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var newSrc = getSrc().specialise(frame);
        if(newSrc instanceof ConstNode){
            var trg = newSrc.execute(null);
            return ConstNode.create(typeUtilNode.getGenericType(trg));
        } else if(newSrc instanceof CachedNode cn) {
            return new CacheNode(FullTypeOfNodeGen.create(cn.liftCache()));
        }  else {
            return FullTypeOfNodeGen.create(newSrc);
        }
    }

    @Override
    public String toString() {
        return "typeOf("+getSrc()+")";
    }

    @Override
    public FullTypeOfNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return FullTypeOfNodeGen.create(getSrc().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).TypeType;
    }

    @Override
    public int complexity() {
        return 1;
    }
}
