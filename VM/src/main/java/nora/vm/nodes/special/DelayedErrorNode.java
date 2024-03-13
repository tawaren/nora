package nora.vm.nodes.special;

import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;


public class DelayedErrorNode extends ExecutionNode {
    private final RuntimeException specExp;

    public DelayedErrorNode(RuntimeException specExp) {
        this.specExp = specExp;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        throw specExp;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new DelayedErrorNode(specExp);
    }
}
