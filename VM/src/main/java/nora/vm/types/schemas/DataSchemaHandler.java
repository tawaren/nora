package nora.vm.types.schemas;

import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.utilities.TriState;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.Type;
import nora.vm.types.schemas.utils.SchemaHandler;

import java.util.HashMap;

@ExportLibrary(InteropLibrary.class)
public abstract class DataSchemaHandler extends SchemaHandler implements TruffleObject {
    private final String fullyQualifiedName;
    private final RuntimeDataFactory factory;
    public final Type type;

    protected DataSchemaHandler(String fullyQualifiedName, RuntimeDataFactory factory, Type type) {
        this.fullyQualifiedName = fullyQualifiedName;
        this.factory = factory;
        this.type = type;
    }

    public RuntimeData create(int dispatchIndex){
        return factory.create(this, dispatchIndex);
    }

    public abstract PropertyManager getProperty(int slot);

    @ExportMessage
    public final boolean isInstantiable(){
        return true;
    }

    @ExportMessage
    public final RuntimeData instantiate(Object... arguments) throws ArityException {
        //Todo: Type checks???
        var props = getAllProperties();
        if(arguments.length != props.length) throw ArityException.create(props.length,props.length, arguments.length);
        var coordinator = NoraVmContext.getDispatchCoordinator(null);
        var raw = factory.create(this, coordinator.assignIndex(type, NoraVmContext.getLoader(null)));
        for(int i = 0; i < props.length;i++){
            props[i].getSetter(ConstNode.create(arguments[i])).executeSet(null, raw);
        }
        return raw;
    }

    @ExportMessage
    public final boolean isMetaObject(){
        return true;
    }

    @ExportMessage
    public final String getMetaQualifiedName(){
        //Todo: Maybe add generic params
        return fullyQualifiedName;
    }
    @ExportMessage
    public final String getMetaSimpleName() {
        return fullyQualifiedName.split("::")[1];
    }

    @ExportMessage
    public final TriState isIdenticalOrUndefined(Object other){
        if(this == other) return TriState.TRUE;
        return TriState.FALSE;
    }

    @ExportMessage
    public final int identityHashCode() {
        return hashCode();
    }

    @ExportMessage
    public final boolean hasLanguage(){
        return true;
    }

    @ExportMessage
    public final Class<? extends TruffleLanguage<?>> getLanguage(){
        return NoraLanguage.class;
    }

    @ExportMessage
    public final Object toDisplayString(boolean allowSideEffects) {
        return fullyQualifiedName;
    }

    @ExportMessage
    public final boolean isMetaInstance(Object instance){
        if(instance instanceof RuntimeData rd){
            return rd.handler == this;
        }
        return false;
    }

    private HashMap<String,PropertyManager> lazyMemberIndex;

    public HashMap<String,PropertyManager> getMemberIndex(){
        if(lazyMemberIndex == null){
            lazyMemberIndex = new HashMap<>();
            for(PropertyManager prop: getAllProperties()){
                lazyMemberIndex.put(prop.getName(),prop);
            }
        }
        return lazyMemberIndex;
    }
}
