package nora.vm.nodes.property.setter;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.data.NoraData;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class IntPropertySetNode extends NoraPropertySetNode {
    private final StaticProperty property;
    @Child NoraNode valueNode;

    public IntPropertySetNode(StaticProperty property, NoraNode valueNode) {
        this.property = property;
        this.valueNode = valueNode;
    }

    @Override
    public NoraNode getValueNode() {
        return valueNode;
    }

    public void executeSet(VirtualFrame virtualFrame, NoraData data) {
        try {
            property.setInt(data, valueNode.executeInt(virtualFrame));
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Mismatch");
        }
    }

    public NoraPropertySetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new IntPropertySetNode(property, valueNode.cloneUninitialized());
    }
}
