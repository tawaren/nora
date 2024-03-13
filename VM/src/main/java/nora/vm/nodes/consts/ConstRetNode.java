package nora.vm.nodes.consts;

import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;


public class ConstRetNode extends ExecutionNode {
    private final EnsureOrRetNode.EarlyReturnException exp;

    public ConstRetNode(EnsureOrRetNode.EarlyReturnException exp) {
        this.exp = exp;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        throw exp;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new ConstRetNode(exp);
    }
}
