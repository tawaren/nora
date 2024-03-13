package nora.vm.nodes.hash_code.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.runtime.data.NoraData;

public abstract class DataHashCodeNode extends Node {
    public abstract int executeHashCode(NoraData data);
    public abstract DataHashCodeNode cloneUninitialized();

    public static DataHashCodeNode createPropertyNode(PropertyManager prop, int recursionDepth){
        CompilerAsserts.neverPartOfCompilation();
        return DataPropertyHashCodeNodeGen.create(prop.getGetter(), recursionDepth+1);
    }

    public static DataHashCodeNode composeNodes(DataHashCodeNode... nodes){
        CompilerAsserts.neverPartOfCompilation();
        return new DataHashCodeComposeNode(nodes);
    }
}
