package nora.vm.nodes.consts;

import nora.vm.nodes.NoraNode;


public class ObjectNode extends ConstNode{
    private final Object value;

    ObjectNode(Object value) {
        if(value instanceof NoraNode) throw new IllegalArgumentException("Object Nodes must be evaluated");
        this.value = value;
    }

    @Override
    Object getConstant() {
        return value;
    }

}
