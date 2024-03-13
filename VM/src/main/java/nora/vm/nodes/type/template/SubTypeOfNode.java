package nora.vm.nodes.type.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.nodes.type.ConcreteTypeOfNodeGen;
import nora.vm.nodes.type.FullInstanceOfNode;
import nora.vm.nodes.type.FullTypeOfNodeGen;
import nora.vm.nodes.type.SimpleInstanceOfNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;


public  class SubTypeOfNode extends TemplateNode {
    private final NoraNode subType;
    private final NoraNode superType;

    public SubTypeOfNode(NoraNode subType, NoraNode superType) {
        this.subType = subType;
        this.superType = superType;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Type newSubType;
        if(subType.specialise(frame) instanceof TypeNode tn){
            newSubType = tn.getType();
        } else {
            throw new IllegalStateException("Dynamic types are not supported");
        }
        Type newSuperType;
        if(superType.specialise(frame) instanceof TypeNode tn2){
            newSuperType = tn2.getType();
        } else {
            throw new IllegalStateException("Dynamic types are not supported");
        }
        //It is always load time evaluatable (not always compiletime)
        return ConstNode.create(newSubType.subTypeOf(newSuperType));
    }

    @Override
    public String toString() {
        return "subTypeOf("+subType+", "+superType+")";
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).BooleanType;
    }
}
