package nora.vm.nodes.property.setter;


import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.data.NoraData;

public abstract class NoraPropertySetNode extends Node {
    public abstract NoraNode getValueNode();

    public abstract void executeSet(VirtualFrame virtualFrame, NoraData data);
    public abstract NoraPropertySetNode cloneUninitialized();
}
