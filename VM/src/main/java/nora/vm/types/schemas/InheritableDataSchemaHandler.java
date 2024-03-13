package nora.vm.types.schemas;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.staticobject.StaticShape;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.types.Type;
import nora.vm.types.schemas.utils.ToStringTemplate;

public abstract class InheritableDataSchemaHandler extends DataSchemaHandler{
    protected final InheritableDataSchemaHandler parent;
    public final StaticShape<RuntimeDataFactory> schema;
    protected final int inheritedProps;

    public InheritableDataSchemaHandler(String fullyQualifiedName, Type typ, InheritableDataSchemaHandler parent, StaticShape<RuntimeDataFactory> schema) {
        super(fullyQualifiedName, schema.getFactory(), typ);
        this.schema = schema;
        this.parent = parent;
        var propInheritance = 0;
        if(parent != null) propInheritance = parent.totalNumProps();
        this.inheritedProps = propInheritance;
    }

    public abstract PropertyManager getLocalProperty(int slot);
    public abstract int getNumLocalProperties();

    @ExplodeLoop
    @Override
    public PropertyManager getProperty(int slot) {
        if(slot < inheritedProps){
            return parent.getProperty(slot);
        } else {
            var relSlot = slot - inheritedProps;
            return getLocalProperty(relSlot);
        }
    }

    public int totalNumProps() {
        return inheritedProps + getNumLocalProperties();
    }

    public int fillProps(int index, PropertyManager[] collector){
        if(parent != null) index = parent.fillProps(index, collector);
        var locals = getNumLocalProperties();
        for(int i = 0; i < locals; i++){
            collector[index+i] = getLocalProperty(i);
        }
        return index+locals;
    }

    @Override
    public PropertyManager[] getAllProperties() {
        var locals = getNumLocalProperties();
        var collector = new PropertyManager[inheritedProps + locals];
        fillProps(0, collector);
        return collector;
    }
}
