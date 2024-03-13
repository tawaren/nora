package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.method.EntryPoint;
import nora.vm.nodes.ClosureNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.types.schemas.ClosureSchemaHandler;
import nora.vm.runtime.data.ClosureData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;

public class GenericClosureNode extends TemplateNode {
    private final EntryPoint code;
    private final NoraNode[] genTypeExprs;
    private final NoraNode funType;
    private final NoraNode[] captures;

    public GenericClosureNode(NoraNode[] genTypeExprs, NoraNode[] captures, EntryPoint code, NoraNode funType) {
        this.genTypeExprs = genTypeExprs;
        this.captures = captures;
        this.code = code;
        this.funType = funType;
    }

    private NoraNode createNode(ClosureSchemaHandler handler, EntryPoint code, NoraNode[] captures, Type funType){
        CompilerAsserts.neverPartOfCompilation();
        var setter = new NoraPropertySetNode[captures.length];
        var allHandlers = handler.getAllCaptures();
        var capturesCached = true;
        for(int i = 0; i < setter.length; i++){
            if(!(captures[i] instanceof CachedNode)) capturesCached = false;
            setter[i] = allHandlers[i].getSetter(captures[i]);
        }
        var clo = new ClosureNode(setter,code,funType, new IdentityObject(), handler);
        if(capturesCached) {
            return new CacheNode(clo);
        }
        return clo;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Type[] captureTypes = new Type[genTypeExprs.length];
        for (int i = 0; i < genTypeExprs.length; i++) {
            NoraNode gen = genTypeExprs[i];
            NoraNode newType = gen.specialise(frame);
            if (newType instanceof TypeNode tn){
                captureTypes[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic types are not supported");
            }
        }

        NoraNode[] newArgs = new NoraNode[captures.length];
        NoraNode newTypeNode = funType.specialise(frame);
        Type funType;
        if(newTypeNode instanceof TypeNode tn){
            funType = tn.getType();
        } else {
            throw new RuntimeException("Dynamic Types are not supported");
        }
        var allConst = true;
        for (int i = 0; i < captures.length; i++) {
            var res = captures[i].specialise(frame);
            newArgs[i] = res;
            if(!(res instanceof ConstNode && allConst)) {
                allConst = false;
            }
        }
        var handler =  getContext().getSchemaManager().getClosureHandlerFor(captureTypes);
        var newCode = code.specialiseClosure(funType.applies, captureTypes, frame.getGenerics());
        if(allConst){
            ClosureData data = handler.create(newCode, funType, new IdentityObject());
            var props = handler.getAllCaptures();
            for (int i = 0; i < newArgs.length; i++) {
                props[i].getSetter(newArgs[i]).executeSet(null,data);
            }
            return ConstNode.create(data);
        } else {
            return createNode(handler, newCode, newArgs, funType);
        }
    }

    @Override
    public String toString() {
        var args = Arrays.stream(captures).map(Object::toString).toList();
        return "closure("+String.join(", ",args)+"){"+code+"}";
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        assert funType instanceof TypeNode;
        return ((TypeNode)funType).getType();
    }
}
