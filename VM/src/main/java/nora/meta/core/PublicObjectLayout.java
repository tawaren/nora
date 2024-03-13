package nora.meta.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.staticobject.StaticShape;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.types.Type;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.DefaultDataSchemaHandler;
import nora.vm.types.schemas.InheritableDataSchemaHandler;
import nora.vm.types.schemas.utils.SchemaOpsHelper;

public class PublicObjectLayout extends InheritableDataSchemaHandler {
    private final PropertyManager[] fields;
    private int implCount = 0;

    public PublicObjectLayout(String fullyQualifiedNam, InheritableDataSchemaHandler parent, StaticShape<RuntimeDataFactory> shape, PropertyManager[] fields, Type type) {
        super(fullyQualifiedNam, type, parent, shape);
        this.fields = fields;
    }

    public int getPrivateImplementationCount() {
        return implCount;
    }

    public int registerPrivate(){
        return implCount++;
    }

    @Override
    public PropertyManager getLocalProperty(int slot) {
        return fields[slot];
    }

    @Override
    public int getNumLocalProperties() {
        return fields.length;
    }

    @ExplodeLoop
    public DataEqNode createEqNode(int recursionDepth) {
        CompilerAsserts.neverPartOfCompilation();
        return SchemaOpsHelper.createEqNode(this, parent, fields, recursionDepth);
    }

    public DataToStringNode createToStringNode(int recursionDepth) {
        return SchemaOpsHelper.createToStringNode(this, DefaultDataSchemaHandler.defaultTemplate, recursionDepth);
    }

    public DataHashCodeNode createHashCodeNode(int recursionDepth) {
        return SchemaOpsHelper.createHashCodeNode(this,parent,fields,recursionDepth);
    }


}
