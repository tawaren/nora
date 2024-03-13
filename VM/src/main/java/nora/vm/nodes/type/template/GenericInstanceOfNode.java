package nora.vm.nodes.type.template;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
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
import nora.vm.types.TypeInfo;
import nora.vm.types.TypeUtil;


public  class GenericInstanceOfNode extends TemplateNode {
    private final NoraNode value;
    private final NoraNode superType;

    public GenericInstanceOfNode(NoraNode value, NoraNode superType) {
        this.value = value;
        this.superType = superType;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        var newValue = value.specialise(frame);
        Type newSuperType;
        if(superType.specialise(frame) instanceof TypeNode tn){
            newSuperType = tn.getType();
        } else {
            throw new IllegalStateException("Dynamic types are not supported");
        }
        Type staticSrcType;
        if(newValue instanceof ConstNode cn){
            staticSrcType = util.extractFullType(cn.executeObject(null));
        } else {
            staticSrcType = newValue.getType(frame);
        }
        boolean isConcrete = staticSrcType.isConcrete();
        if(staticSrcType.subTypeOf(newSuperType)) return ConstNode.create(true);
        //if it is concrete then we now statically exactly the same as dynamically
        // Further, it covers newValue instanceof ConstNode (as those are always concrete
        if(isConcrete || !staticSrcType.isRelatedTo(newSuperType)) return ConstNode.create(false);
        //Check if we can use a quick check
        if(Type.appliesSubType(staticSrcType.applies, newSuperType.applies)){
            if(newValue instanceof CachedNode cn){
                var indexNode = ConcreteTypeOfNodeGen.create(cn.liftCache());
                return new CacheNode(new SimpleInstanceOfNode(indexNode, newSuperType.info));
            }
            var indexNode = ConcreteTypeOfNodeGen.create(newValue);
            return new SimpleInstanceOfNode(indexNode, newSuperType.info);
        }

        if(newValue instanceof CachedNode cn){
            var indexNode = FullTypeOfNodeGen.create(cn.liftCache());
            return new CacheNode(new FullInstanceOfNode(indexNode, newSuperType));
        }
        var indexNode = FullTypeOfNodeGen.create(newValue);
        return new FullInstanceOfNode(indexNode, newSuperType);
    }

    @Override
    public String toString() {
        return "subtypeOf("+value+", "+superType+")";
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).BooleanType;
    }
}
