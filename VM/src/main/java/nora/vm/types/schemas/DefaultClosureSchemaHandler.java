package nora.vm.types.schemas;

import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import nora.vm.nodes.equality.ClosureIdentityEqNode;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.hash_code.ClosureIdentityHashCodeNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.nodes.property.PropertyManager;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.data.ClosureDataFactory;

public class DefaultClosureSchemaHandler extends ClosureSchemaHandler{
    @CompilationFinal(dimensions = 1)
    protected final PropertyManager[] captures;

    public DefaultClosureSchemaHandler(ClosureDataFactory factory, PropertyManager[] captures) {
        super(factory);
        this.captures = captures;
    }

    @Override
    public PropertyManager getCapture(int slot) {
        return captures[slot];
    }

    @Override
    public PropertyManager getProperty(int slot) {
        return getCapture(slot);
    }

    @Override
    public PropertyManager[] getAllCaptures() {
        return captures;
    }

    @Override
    public PropertyManager[] getAllProperties() {
        return captures;
    }

    @Override
    public DataEqNode createEqNode(int recursionDepth) {
        var cmpNodes = new DataEqNode[captures.length];
        for(int i = 0; i < cmpNodes.length; i++){
            cmpNodes[i] = DataEqNode.createPropertyNode(captures[i], recursionDepth);
        }
        var propsNode = DataEqNode.composeNodes(cmpNodes);
        var identityNode = new ClosureIdentityEqNode();
        return DataEqNode.composeNodes(identityNode, propsNode);
    }

    @Override
    public DataToStringNode createToStringNode(int recursionDepth) {
        return new ClosureToStringNode();
    }

    @Override
    public DataHashCodeNode createHashCodeNode(int recursionDepth) {
        var cmpNodes = new DataHashCodeNode[captures.length];
        for(int i = 0; i < cmpNodes.length; i++){
            cmpNodes[i] = DataHashCodeNode.createPropertyNode(captures[i], recursionDepth);
        }
        var propsNode = DataHashCodeNode.composeNodes(cmpNodes);
        var identityNode = new ClosureIdentityHashCodeNode();
        return DataHashCodeNode.composeNodes(identityNode, propsNode);
    }
}
