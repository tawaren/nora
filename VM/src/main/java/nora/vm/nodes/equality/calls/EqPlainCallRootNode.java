package nora.vm.nodes.equality.calls;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.equality.EqPlainNode;
import nora.vm.truffle.NoraLanguage;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class EqPlainCallRootNode extends RootNode {
    @Child public NoraNode arg1;
    @Child public NoraNode arg2;
    @Child public EqPlainNode body;

    public EqPlainCallRootNode(NoraNode arg1, NoraNode arg2, EqPlainNode body) {
        super(NoraLanguage.get(null), FrameDescriptor.newBuilder(0).build());
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            var a1 = arg1.executeObject(frame);
            var a2 = arg2.executeObject(frame);
            return body.execute(a1,a2);
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
    public EqPlainCallRootNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new EqPlainCallRootNode(arg1.cloneUninitialized(), arg2.cloneUninitialized(), body.cloneUninitialized());
    }

    @Override
    public String toString() {
        return "OptimizedPlainEq";
    }

    @Override
    public String getName() {
        return "OptimizedPlainEq";
    }
}
