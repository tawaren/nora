package nora.vm.nodes.arr;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.method.DispatchNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

//Todo: needs a template that produces call Node
@NodeChild(value = "array", type = NoraNode.class)
@NodeChild(value = "initial", type = NoraNode.class)
@NodeChild(value = "function", type = NoraNode.class)
@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
public abstract class ArrayFoldRNode extends ExecutionNode {
    protected abstract NoraNode getInitial();
    protected abstract NoraNode getArray();
    protected abstract NoraNode getFunction();
    @Child private DispatchNode callNode;

    public ArrayFoldRNode(DispatchNode callNode) {
        this.callNode = callNode;
    }

    public Object evalFunction(Object function, Object state, Object elem) {
        var args = new Object[3];
        args[0] = function;
        args[1] = elem;
        args[2] = state;
        CompilerAsserts.partialEvaluationConstant(args.length);
        return callNode.executeDispatch(function,args);
    }

    @Specialization
    public Object foldBool(boolean[] arr, Object state, Object function){
        for (int i = arr.length-1; i >= 0; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldByte(byte[] arr, Object state, Object function){
        for (int i = arr.length-1; i >= 0; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldInt(int[] arr, Object state, Object function){
        for (int i = arr.length-1; i >= 0; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldLong(long[] arr, Object state, Object function){
        for (int i = arr.length-1; i >= 0; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldObject(Object[] arr, Object state, Object function){
        for (int i = arr.length-1; i >= 1; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Override
    public Type getType(SpecFrame frame) {
        return getInitial().getType(frame);
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayFoldRNodeGen.create(callNode.cloneUninitialized(), getArray().cloneUninitialized(), getInitial().cloneUninitialized(), getFunction().cloneUninitialized());
    }

}
