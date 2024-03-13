package nora.vm.nodes.method;


public abstract class DispatchNode extends CallSideNode {
    public abstract Object executeDispatch(Object target, Object[] arguments);
    public abstract DispatchNode cloneUninitialized();
}
