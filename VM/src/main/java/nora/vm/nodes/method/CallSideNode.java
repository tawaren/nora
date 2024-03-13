package nora.vm.nodes.method;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.DirectCallNode;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.method.lookup.MethodLookup;

public class CallSideNode extends Node {

    protected final IndirectCallNode initIndirectCall(){
        return IndirectCallNode.create();
    }

    protected final DirectCallNode initDirectCall(CallTarget target){
        return DirectCallNode.create(target);
    }

    protected final DirectCallNode initDirectCall(MethodLookup target, Object[] args, int[] types){
        return initDirectCall(target.runtimeLookup(args,types).getTarget());
    }

    public final Object callDirect(DirectCallNode node, Object[] arguments) {
        return node.call(arguments);
    }

    public final Object callIndirect(IndirectCallNode node, CallTarget target, Object[] arguments) {
        return node.call(target, arguments);
    }
}
