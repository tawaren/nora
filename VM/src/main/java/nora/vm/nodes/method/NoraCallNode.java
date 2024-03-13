package nora.vm.nodes.method;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.method.Callable;
import nora.vm.method.Method;
import nora.vm.method.MultiMethod;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;

public class NoraCallNode extends NoraNode {
    @Child private NoraNode target;
    @Children private final NoraNode[] arguments;
    @Child private DispatchNode node;

    public NoraCallNode(NoraNode target, NoraNode[] arguments, DispatchNode node) {
        this.target = target;
        this.arguments = arguments;
        this.node = node;
    }


    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame virtualFrame) {
        var trg = target.execute(virtualFrame);
        CompilerAsserts.partialEvaluationConstant(arguments.length);
        var args = new Object[arguments.length+1];
        args[0] = trg;
        CompilerAsserts.partialEvaluationConstant(args.length);
        for (int i = 1; i < args.length; i++) args[i] = arguments[i-1].execute(virtualFrame);
        return node.executeDispatch(trg,args);
    }

    public static NoraCallNode createDirectMethodCall(Method method, NoraNode[] arguments){
        CompilerAsserts.neverPartOfCompilation();
        return new NoraCallNode(ConstNode.create(method), arguments, DirectCallableDispatchNodeGen.create());
    }

    public static NoraCallNode createDirectClosureCall(ClosureData method, NoraNode[] arguments){
        CompilerAsserts.neverPartOfCompilation();
        return new NoraCallNode(ConstNode.create(method), arguments, DirectCallableDispatchNodeGen.create());
    }

    public static NoraCallNode createLookupCall(MethodLookup lookup, NoraNode[] arguments){
        CompilerAsserts.neverPartOfCompilation();
        return new NoraCallNode(ConstNode.create(lookup), arguments, new TypingDispatchNode(MethodLookupDispatchNodeGen.create(3), lookup.dispatchedArgs()));
    }

    public static NoraCallNode createDynamicCall(NoraNode target, NoraNode[] arguments){
        CompilerAsserts.neverPartOfCompilation();
        return new NoraCallNode(target, arguments, FlexibleDispatchNodeGen.create());
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        //This is already specialised
        return this;
    }

    @Override
    public String toString() {
        return "Call["+node+":"+target+"]("+ String.join(", ", Arrays.stream(arguments).map(Node::toString).toList())+")";
    }

    @Override
    public NoraCallNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        var args = new NoraNode[arguments.length];
        for(int i = 0; i < args.length; i++) args[i] = arguments[i].cloneUninitialized();
        return new NoraCallNode(target.cloneUninitialized(), args, node.cloneUninitialized());
    }


    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        Type tp = null;
        if(target instanceof ConstNode){
            var res = target.execute(null);
            if(res instanceof Callable c) tp = c.getType();
            if(res instanceof MultiMethod mm) tp = mm.getType();
        } else {
            //Todo: This one can actually lead to 2 calls to getType() on target
            //      In theory this can have non Constant runtime
            tp = target.getType(frame);
        }
        assert tp != null;
        var appls = tp.applies;
        return appls[appls.length-1].type();
    }

    @Override
    public boolean isUnboxed() {
        return false;
    }
}
