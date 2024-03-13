package nora.vm.nodes.string.to_string.calls;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.string.to_string.ToStringPlainNode;
import nora.vm.nodes.string.to_string.data.DataToStringNode;
import nora.vm.runtime.data.NoraData;
import nora.vm.truffle.NoraLanguage;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ToStringPlainCallRootNode extends RootNode {

    @Child public NoraNode arg1;
    @Child public NoraNode arg2;
    @Child public ToStringPlainNode body;

    public ToStringPlainCallRootNode(NoraNode arg1, NoraNode arg2, ToStringPlainNode body) {
        super(NoraLanguage.get(null), FrameDescriptor.newBuilder(0).build());
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.body = body;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        try {
            var builder = arg1.execute(frame);
            var data = (NoraData)arg2.executeObject(frame);
            body.execute((TruffleStringBuilder) builder,data);
            return null;
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
    public ToStringPlainCallRootNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new ToStringPlainCallRootNode(arg1.cloneUninitialized(), arg2.cloneUninitialized(), body.cloneUninitialized());
    }

    @Override
    public String getName() {
        return "OptimizedDataToString";
    }

}
