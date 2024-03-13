package nora.vm.nodes;

import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.specTime.SpecFrame;

public abstract class StmNode extends NoraNode{
    public abstract void executeVoid(VirtualFrame frame);

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        executeVoid(virtualFrame);
        return null;
    }

    @Override
    public abstract StmNode cloneUninitialized();

    @Override
    public abstract StmNode specialise(SpecFrame frame) throws Exception;
}
