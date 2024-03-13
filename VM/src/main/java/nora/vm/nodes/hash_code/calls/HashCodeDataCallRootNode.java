package nora.vm.nodes.hash_code.calls;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.hash_code.data.DataHashCodeNode;
import nora.vm.truffle.NoraLanguage;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class HashCodeDataCallRootNode extends RootNode {

    @Child public NoraNode arg1;
    @Child public DataHashCodeNode body;

    public HashCodeDataCallRootNode(NoraNode arg1, DataHashCodeNode body) {
        super(NoraLanguage.get(null), FrameDescriptor.newBuilder(0).build());
        this.arg1 = arg1;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            var data = arg1.executeNoraData(frame);
            return body.executeHashCode(data);
        } catch (UnexpectedResultException | ClassCastException e) {
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
    public HashCodeDataCallRootNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new HashCodeDataCallRootNode(arg1.cloneUninitialized(), body.cloneUninitialized());
    }


    @Override
    public String toString() {
        return "OptimizedDataHashCode";
    }

    @Override
    public String getName() {
        return "OptimizedDataHashCode";
    }

}
