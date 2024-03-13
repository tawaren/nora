package nora.vm.nodes.arr;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.method.DispatchNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

//Todo: needs a template that produces call Node
@NodeChild(value = "initial", type = NoraNode.class)
@NodeChild(value = "array", type = NoraNode.class)
@NodeChild(value = "function", type = NoraNode.class)
@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
public abstract class ArrayFoldLNode extends ExecutionNode {
    protected abstract NoraNode getInitial();
    protected abstract NoraNode getArray();
    protected abstract NoraNode getFunction();
    @Child private DispatchNode callNode;

    public ArrayFoldLNode(DispatchNode callNode) {
        this.callNode = callNode;
    }

    public Object evalFunction(Object function, Object state, Object elem) {
        var args = new Object[3];
        args[0] = function;
        args[1] = state;
        args[2] = elem;
        CompilerAsserts.partialEvaluationConstant(args.length);
        return callNode.executeDispatch(function,args);
    }

    @Specialization
    public Object foldBool(Object state, boolean[] arr, Object function){
        for (int i = 0; i < arr.length; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldByte(Object state, byte[] arr, Object function){
        for (int i = 0; i < arr.length; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldInt(Object state, int[] arr, Object function){
        for (int i = 0; i < arr.length; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldLong(Object state, long[] arr, Object function){
        for (int i = 0; i < arr.length; i++){
            state = evalFunction(function, state, arr[i]);
        }
        return state;
    }

    @Specialization
    public Object foldObject(Object state, Object[] arr, Object function){
        for (int i = 1; i < arr.length; i++){
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
        return ArrayFoldLNodeGen.create(callNode.cloneUninitialized(), getInitial().cloneUninitialized(), getArray().cloneUninitialized(), getFunction().cloneUninitialized());
    }

}
