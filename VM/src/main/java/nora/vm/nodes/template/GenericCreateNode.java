package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.CreateNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ObjectNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;

public class GenericCreateNode extends TemplateNode {
    private final String id;
    private final NoraNode[] genTypeExprs;
    private final NoraNode[] arguments;
    public GenericCreateNode(String id, NoraNode[] genTypeExprs, NoraNode[] arguments) {
        this.id = id;
        this.genTypeExprs = genTypeExprs;
        this.arguments = arguments;
    }

    private NoraNode createNode(DataSchemaHandler handler, int dispatcherIndex, NoraNode[] newArgs){
        CompilerAsserts.neverPartOfCompilation();
        var setter = new NoraPropertySetNode[newArgs.length];
        var allHandlers = handler.getAllProperties();
        for(int i = 0; i < setter.length; i++){
            setter[i] = allHandlers[i].getSetter(newArgs[i]);
        }
        return new CreateNode(dispatcherIndex, handler, setter);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Type[] generics = new Type[genTypeExprs.length];
        for (int i = 0; i < genTypeExprs.length; i++) {
            NoraNode gen = genTypeExprs[i];
            NoraNode newType = gen.specialise(frame);
            if (newType instanceof TypeNode tn){
                generics[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic types are not supported");
            }

        }
        NoraNode[] newArgs = new NoraNode[arguments.length];
        var allCached = true;
        var allConst = true;
        for (int i = 0; i < arguments.length; i++) {
            newArgs[i] = arguments[i].specialise(frame);
            if(!(newArgs[i] instanceof ConstNode)) allConst = false;
            if(!(newArgs[i] instanceof CachedNode)) allCached = false;
        }

        var handler =  getContext().getSchemaManager().getDataHandlerFor(id, generics);
        var coordinator = NoraVmContext.getDispatchCoordinator(null);
        int dispatcherIndex = coordinator.assignIndex(handler.type, getContext().getLoader());
        if(allConst){
            RuntimeData data = handler.create(dispatcherIndex);
            var props = handler.getAllProperties();
            assert props.length == arguments.length;
            for (int i = 0; i < arguments.length; i++) {
                props[i].getSetter(newArgs[i]).executeSet(null,data);
            }
            return ConstNode.create(data);
        } else if(allCached) {
            return new CacheNode(createNode(handler, dispatcherIndex, newArgs));
        } else {
            return createNode(handler, dispatcherIndex, newArgs);
        }
    }

    @Override
    public String toString() {
        var args = Arrays.stream(arguments).map(Object::toString).toList();
        var genTypes = Arrays.stream(genTypeExprs).map(Object::toString).toList();
        return id+"["+String.join(", ",genTypes)+"]("+String.join(", ",args)+"}";
    }
}
