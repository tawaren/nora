package nora.meta.core;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.method.Method;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.RuntimeDataFactory;
import nora.vm.types.Type;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.DefaultDataSchemaHandler;
import nora.vm.types.schemas.utils.SchemaOpsHelper;

public class PrivateObjectLayout extends DataSchemaHandler {
    private final PublicObjectLayout parent;
    private final Type[] allTypeParams;
    private final PropertyManager[] fields;
    private final int dispatchId;

    private final String methodSuffix;
    //Todo: add a method list to the file and get it here
    //      allowing to export them as member in Interop protocol
    public PrivateObjectLayout(String fullyQualifiedName, PublicObjectLayout parent, RuntimeDataFactory factory, Type[] allTypeParams,  PropertyManager[] fields) {
        //Todo: get a name here
        super(fullyQualifiedName, factory,parent.type);
        this.dispatchId = parent.registerPrivate();
        this.parent = parent;
        this.allTypeParams = allTypeParams;
        this.fields = fields;
        this.methodSuffix = fullyQualifiedName.replace(".","_").replace("::", "__");
    }

    public String getMethodSuffix() {
        return methodSuffix;
    }

    @Override
    public PropertyManager getProperty(int slot) {
        return fields[slot];
    }

    @Override
    public PropertyManager[] getAllProperties() {
        return fields;
    }
    //Todo: Note: We can define External now
    // object method[T] MyTraitObject[Pub1,Pub2|Sec1]#test(....) = {
    //   Their is an implicit this:MyTraitObject[Pub1,Pub2|Sec1] for field access
    //   If it leaves context it reverts to MyTraitObject[Pub1,Pub2]
    //   Pub1,Pub2 can be concrete like String
    //   Sec1 can not be concrete
    // }
    // Can be called: obj#test[T](....) -- where obj is MyTraitObject[Pub1,Pub2]
    // Fully qualified signature is: modulePath::test[T,Pub1,Pub2,Sec1] <- note pubs may be more specific
    // Todo: Performance Note:
    // Note: with this design even normal objects can have methods
    //       object method[T] NormalStruct[Pub1,Pub2]#test(....)
    //       sadly this requires them to use ObjectLayerHandler
    //        supports only private yet, but easy adaptable (just make public concrete for those cases)
    public Type[] fullObjectSignature(){
        return allTypeParams;
    }

    public int getDispatchId() {
        return dispatchId;
    }

    public DataEqNode createEqNode(int recursionDepth) {
        CompilerAsserts.neverPartOfCompilation();
        return SchemaOpsHelper.createEqNode(this, parent, fields, recursionDepth);
    }

    public DataToStringNode createToStringNode(int recursionDepth) {
        return SchemaOpsHelper.createToStringNode(this, DefaultDataSchemaHandler.defaultTemplate, recursionDepth);
    }

    @ExplodeLoop
    public DataHashCodeNode createHashCodeNode(int recursionDepth) {
        return SchemaOpsHelper.createHashCodeNode(this,parent,fields,recursionDepth);
    }
}
