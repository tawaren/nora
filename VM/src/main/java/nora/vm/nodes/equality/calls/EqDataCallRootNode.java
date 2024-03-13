package nora.vm.nodes.equality.calls;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.truffle.NoraLanguage;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class EqDataCallRootNode extends RootNode {
    @Child public NoraNode arg1;
    @Child public NoraNode arg2;
    @Child public DataEqNode body;

    public EqDataCallRootNode(NoraNode arg1, NoraNode arg2, DataEqNode body) {
        super(NoraLanguage.get(null), FrameDescriptor.newBuilder(0).build());
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            var a1 = arg1.executeNoraData(frame);
            var a2 = arg2.executeNoraData(frame);
            return body.executeDataEq(a1,a2);
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Missmatch");
        }
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    protected boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override
    public EqDataCallRootNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new EqDataCallRootNode(arg1.cloneUninitialized(), arg2.cloneUninitialized(), body.cloneUninitialized());
    }

    @Override
    public String toString() {
        return "OptimizedDataEq";
    }

    @Override
    public String getName() {
        return "OptimizedDataEq";
    }
}
