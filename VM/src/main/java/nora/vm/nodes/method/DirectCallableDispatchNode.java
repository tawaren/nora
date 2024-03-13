package nora.vm.nodes.method;


import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
//import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.DirectCallNode;
import nora.vm.method.Callable;

@GenerateInline(false)
public abstract class DirectCallableDispatchNode extends DispatchNode {

    @Specialization
    protected Object callStatic(Callable method, Object[] args,
                                @Cached(neverDefault = true, value = "initDirectCall(method.getTarget())") DirectCallNode callNode) {
        CompilerAsserts.partialEvaluationConstant(method);
        return callDirect(callNode, args);
    }

    @Override
    public String toString() {
        return "DirectClosureDispatch";
    }

    @Override
    public DirectCallableDispatchNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DirectCallableDispatchNodeGen.create();
    }
}
