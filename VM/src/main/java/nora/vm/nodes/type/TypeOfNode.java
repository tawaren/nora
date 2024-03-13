package nora.vm.nodes.type;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.CreateNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.ObjectNode;
import nora.vm.nodes.utils.TypeOfUtilNode;
import nora.vm.nodes.utils.TypeOfUtilNodeGen;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

@NodeChild(value = "src", type = NoraNode.class)
@ReportPolymorphism
public abstract class TypeOfNode extends NoraNode {
    @Child private TypeOfUtilNode typeUtilNode = TypeOfUtilNodeGen.create();
    abstract NoraNode getSrc();

    @Specialization
    public TypeInfo typeOf(byte i){
        return typeUtilNode.executeTypeOf(i);
    }

    @Specialization
    public TypeInfo typeOf(int i){
        return typeUtilNode.executeTypeOf(i);
    }

    @Specialization
    public TypeInfo typeOf(long l){
        return typeUtilNode.executeTypeOf(l);
    }

    @Specialization
    public TypeInfo typeOf(boolean b){
        return typeUtilNode.executeTypeOf(b);
    }

    @Specialization
    public TypeInfo typeOf(Object obj){
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
            return new CacheNode(TypeOfNodeGen.create(cn.liftCache()));
        }  else {
            return TypeOfNodeGen.create(newSrc);
        }
    }

    @Override
    public String toString() {
        return "typeOf("+getSrc()+")";
    }

    @Override
    public TypeOfNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return TypeOfNodeGen.create(getSrc().cloneUninitialized());
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
