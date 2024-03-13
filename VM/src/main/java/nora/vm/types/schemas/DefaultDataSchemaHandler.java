package nora.vm.types.schemas;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.staticobject.StaticShape;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.types.Type;
import nora.vm.types.schemas.utils.SchemaOpsHelper;
import nora.vm.types.schemas.utils.ToStringTemplate;

@ExportLibrary(InteropLibrary.class)
public class DefaultDataSchemaHandler extends InheritableDataSchemaHandler{

    @CompilationFinal(dimensions = 1)
    protected final PropertyManager[] props;

    public DefaultDataSchemaHandler(String fullyQualifiedName, Type typ, InheritableDataSchemaHandler parent, StaticShape<RuntimeDataFactory> schema, PropertyManager[] props) {
        super(fullyQualifiedName, typ, parent, schema);
        this.props = props;
    }

    public PropertyManager getLocalProperty(int slot){
        return props[slot];
    }

    @Override
    public int getNumLocalProperties() {
        return props.length;
    }

    public static ToStringTemplate defaultTemplate = new ToStringTemplate(
            TruffleString.FromJavaStringNode.getUncached().execute("{",TruffleString.Encoding.UTF_16),
            TruffleString.FromJavaStringNode.getUncached().execute(", ",TruffleString.Encoding.UTF_16),
            TruffleString.FromJavaStringNode.getUncached().execute("}",TruffleString.Encoding.UTF_16)
    );


    public DataEqNode createEqNode(int recursionDepth) {
        CompilerAsserts.neverPartOfCompilation();
        return SchemaOpsHelper.createEqNode(this, parent, props, recursionDepth);
    }

    @Override
    public DataHashCodeNode createHashCodeNode(int recursionDepth) {
       return SchemaOpsHelper.createHashCodeNode(this,parent,props,recursionDepth);
    }

    public DataToStringNode createToStringNode(int recursionDepth) {
        return SchemaOpsHelper.createToStringNode(this, defaultTemplate, recursionDepth);
    }

    @ExportMessage
    public boolean hasMetaParents(){
        return parent != null;
    }

    @ExportMessage
    public InheritableDataSchemaHandler[] getMetaParents() throws UnsupportedMessageException {
        if(parent == null) throw UnsupportedMessageException.create();
        return new InheritableDataSchemaHandler[]{parent};
    }

}
