package nora.vm.nodes.method;


public abstract class TypedDispatchNode extends CallSideNode {
    protected abstract Object executeDispatch(Object target, Object[] arguments, int[] argTypes);
    public abstract TypedDispatchNode cloneUninitialized();

}
