package nora.meta.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;
//Todo: Abstract does over a factory
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.types.schemas.utils.SchemaOpsHelper;
import nora.vm.types.schemas.utils.ToStringTemplate;
import nora.vm.types.Type;

public class TupleLayout extends DataSchemaHandler {

    private static ToStringTemplate tupleTemplate = new ToStringTemplate(
            TruffleString.FromJavaStringNode.getUncached().execute("(",TruffleString.Encoding.UTF_16),
            TruffleString.FromJavaStringNode.getUncached().execute(", ",TruffleString.Encoding.UTF_16),
            TruffleString.FromJavaStringNode.getUncached().execute(")",TruffleString.Encoding.UTF_16)
    );

    private final PropertyManager[] fields;

    public TupleLayout(RuntimeDataFactory factory, PropertyManager[] fields, Type type) {
        super(TupleLayoutHandler.tupleIdentifier, factory,type);
        this.fields = fields;
    }

    @Override
    public PropertyManager getProperty(int slot) {
        return fields[slot];
    }

    @Override
    public PropertyManager[] getAllProperties() {
        return fields;
    }

    public DataEqNode createEqNode(int recursionDepth) {
        CompilerAsserts.neverPartOfCompilation();
        return SchemaOpsHelper.createEqNode(this, null, fields, recursionDepth);
    }

    @ExplodeLoop
    public DataHashCodeNode createHashCodeNode(int recursionDepth) {
        return SchemaOpsHelper.createHashCodeNode(this,null,fields,recursionDepth);
    }

    public DataToStringNode createToStringNode(int recursionDepth) {
        return SchemaOpsHelper.createToStringNode(this, tupleTemplate, recursionDepth);
    }
}
