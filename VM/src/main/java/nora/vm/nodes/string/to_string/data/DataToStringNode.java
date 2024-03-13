package nora.vm.nodes.string.to_string.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.string.to_string.data.DataPropertyToStringNodeGen;
import nora.vm.nodes.string.to_string.data.DataToStringComposeNodeGen;
import nora.vm.runtime.data.NoraData;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.types.schemas.utils.ToStringTemplate;


public abstract class DataToStringNode extends Node {
    public abstract void executeToString(TruffleStringBuilder builder, NoraData data);
    public abstract DataToStringNode cloneUninitialized();

    public static DataToStringNode createPropertyNode(PropertyManager prop, int recursionDepth){
        CompilerAsserts.neverPartOfCompilation();
        return DataPropertyToStringNodeGen.create(prop.getGetter(), recursionDepth+1);
    }

    public static DataToStringNode composeNodes(ToStringTemplate template, DataToStringNode... nodes){
        CompilerAsserts.neverPartOfCompilation();
        return DataToStringComposeNodeGen.create(nodes, template);
    }
}
