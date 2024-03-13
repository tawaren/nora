package nora.vm.nodes.property.setter;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.data.NoraData;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class NumericPropertySetNode extends NoraPropertySetNode {
    private final StaticProperty longProp;
    private final StaticProperty bigProp;
    @Child NoraNode valueNode;
    @CompilationFinal boolean bigSeen = false;

    public NumericPropertySetNode(StaticProperty longProp, StaticProperty bigProp, NoraNode valueNode) {
        this.longProp = longProp;
        this.bigProp = bigProp;
        this.valueNode = valueNode;
    }

    @Override
    public NoraNode getValueNode() {
        return valueNode;
    }

    @Override
    public void executeSet(VirtualFrame virtualFrame, NoraData data) {
        if(!bigSeen){
            try {
                longProp.setLong(data, valueNode.executeLong(virtualFrame));
            } catch (UnexpectedResultException e) {
                transferToInterpreterAndInvalidate();
                bigSeen = true;
                bigProp.setObject(data, e.getResult());
            }
        } else {
            var value = valueNode.execute(virtualFrame);
            if(value instanceof Long l) {
                longProp.setLong(data, l);
            } else {
                bigProp.setObject(data, value);
            }
        }
    }

    public NoraPropertySetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new NumericPropertySetNode(longProp, bigProp, valueNode.cloneUninitialized());
    }
}
