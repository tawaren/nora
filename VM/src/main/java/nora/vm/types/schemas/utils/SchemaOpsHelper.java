package nora.vm.types.schemas.utils;

import com.oracle.truffle.api.CompilerDirectives;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.InheritableDataSchemaHandler;

public class SchemaOpsHelper {

    private static int recursionIndex(DataSchemaHandler self, PropertyManager[] props) {
        //For now is only active in concretes
        //    in theory we could activate for parent but we need to guard against multi recursion then (parent + child)
        var info = self.type.info;
        if(!info.isConcrete()) return -1;
        for(int i = 0; i < props.length; i++) {
            var t = props[i].getGetter().getPropertyType().info;
            if(t.cat() == info.cat() && t.isAssignableFromConcrete(info.id())) return i;
        }
        return -1;
    }

    //Todo: It seems slightly faster but not by much
    @CompilerDirectives.TruffleBoundary
    private static DataEqNode createEqLoopNode(DataSchemaHandler self, InheritableDataSchemaHandler parent, PropertyManager[] props, int recursionDepth, int recursionIndex) {
        DataEqNode[] cmpNodes  = new DataEqNode[props.length-1];
        NoraPropertyGetNode recursionProp = props[recursionIndex].getGetter();
        DataEqNode fallback = DataEqNode.createPropertyNode(props[recursionIndex], recursionDepth);;
        int recOffset = 0;
        for(int i = 0; i < props.length; i++){
            if(recursionIndex == i){
                recOffset = 1;
            } else {
                cmpNodes[i-recOffset] = DataEqNode.createPropertyNode(props[i], recursionDepth);
            }
        }
        var propsNode = DataEqNode.composeNodes(cmpNodes);
        if(parent != null) propsNode = DataEqNode.composeNodes(parent.createEqNode(recursionDepth), propsNode);
        if(recursionProp == null) return propsNode;
        return DataEqNode.createLoopNode(propsNode, recursionProp, fallback, self);
    }

    @CompilerDirectives.TruffleBoundary
    public static DataEqNode createEqNode(DataSchemaHandler self, InheritableDataSchemaHandler parent, PropertyManager[] props, int recursionDepth) {
        int recursionIndex = recursionIndex(self, props);
        if(recursionIndex != -1) return createEqLoopNode(self, parent, props,recursionDepth, recursionIndex);
        DataEqNode[] cmpNodes = new DataEqNode[props.length];
        for(int i = 0; i < props.length; i++){
            cmpNodes[i] = DataEqNode.createPropertyNode(props[i], recursionDepth);
        }
        var propsNode = DataEqNode.composeNodes(cmpNodes);
        if(parent == null) return propsNode;
        return DataEqNode.composeNodes(parent.createEqNode(recursionDepth), propsNode);
    }

    @CompilerDirectives.TruffleBoundary
    public static DataHashCodeNode createHashCodeNode(DataSchemaHandler self, InheritableDataSchemaHandler parent, PropertyManager[] props, int recursionDepth) {
        var cmpNodes = new DataHashCodeNode[props.length];
        for(int i = 0; i < cmpNodes.length; i++){
            cmpNodes[i] = DataHashCodeNode.createPropertyNode(props[i], recursionDepth);
        }
        var propsNode = DataHashCodeNode.composeNodes(cmpNodes);
        if(parent == null) return propsNode;
        var parentNode = parent.createHashCodeNode(recursionDepth);
        return DataHashCodeNode.composeNodes(parentNode, propsNode);
    }


    @CompilerDirectives.TruffleBoundary
    public static DataToStringNode createToStringNode(DataSchemaHandler self, ToStringTemplate template, int recursionDepth) {
        var all = self.getAllProperties();
        //Todo: we could go parent if the template had a disabale empty property
        var strNodes = new DataToStringNode[all.length];
        for(int i = 0; i < strNodes.length; i++){
            strNodes[i] = DataToStringNode.createPropertyNode(all[i], recursionDepth);
        }
        return DataToStringNode.composeNodes(template,strNodes);
    }
}
