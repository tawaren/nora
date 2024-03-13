package nora.vm.nodes.property.setter;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.data.NoraData;

public class ObjectPropertySetNode extends NoraPropertySetNode {
    private final StaticProperty property;
    @Child NoraNode valueNode;

    public ObjectPropertySetNode(StaticProperty property, NoraNode valueNode) {
        this.property = property;
        this.valueNode = valueNode;
    }

    @Override
    public NoraNode getValueNode() {
        return valueNode;
    }

    @Override
    public void executeSet(VirtualFrame virtualFrame, NoraData data) {
        var res= valueNode.execute(virtualFrame);
        property.setObject(data, res);
    }

    @Override
    public NoraPropertySetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new ObjectPropertySetNode(property, valueNode.cloneUninitialized());
    }
}
