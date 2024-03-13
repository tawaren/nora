package nora.vm.nodes.equality.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.schemas.DataSchemaHandler;

public abstract class DataEqNode extends Node {
    public abstract boolean executeDataEq(NoraData d1, NoraData d2);
    public abstract DataEqNode cloneUninitialized();

    public static DataEqNode createPropertyNode(PropertyManager prop, int recursionDepth){
        CompilerAsserts.neverPartOfCompilation();
        return DataPropertyEqNodeGen.create(prop.getGetter(), recursionDepth);
    }

    public static DataEqNode composeNodes(DataEqNode... nodes){
        CompilerAsserts.neverPartOfCompilation();
        return new DataEqComposeNode(nodes);
    }

    public static DataEqNode createLoopNode(DataEqNode main, NoraPropertyGetNode recursionProp, DataEqNode fallback, DataSchemaHandler loopCheck) {
        CompilerAsserts.neverPartOfCompilation();
        return new DataEqLoopNode(main, recursionProp, fallback, loopCheck);
    }

}
