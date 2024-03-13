package nora.vm.nodes.interop;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.interop.UnsupportedTypeException;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ForeignCall extends ExecutionNode {
    @Children private final NoraNode[] arguments;
    @Child private InteropLibrary targLib;
    private final Object target;

    public ForeignCall(NoraNode[] arguments, Object target, InteropLibrary targLib) {
        this.arguments = arguments;
        this.target = target;
        this.targLib = targLib;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame virtualFrame) {
        var nArgs = new Object[arguments.length];
        CompilerAsserts.partialEvaluationConstant(arguments.length);
        for (int i = 0; i < arguments.length; i++){
            nArgs[i] = arguments[i].execute(virtualFrame);
        }
        try {
            if(targLib.isExecutable(target)){
                return targLib.execute(target, nArgs);
            } else {
                return targLib.instantiate(target, nArgs);
            }
        } catch (UnsupportedTypeException | ArityException | UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error", e);
        }
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        var nArgs = new NoraNode[arguments.length];
        for (int i = 0; i < nArgs.length;i++){
            nArgs[i] = arguments[i].cloneUninitialized();
        }
        return new ForeignCall(nArgs,target,targLib);
    }
}
