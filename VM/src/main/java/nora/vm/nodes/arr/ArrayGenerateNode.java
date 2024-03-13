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
@NodeChild(value = "length", type = NoraNode.class)
@NodeChild(value = "function", type = NoraNode.class)
@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
@ReportPolymorphism
public abstract class ArrayGenerateNode extends ExecutionNode {
    protected abstract NoraNode getLength();
    protected abstract NoraNode getFunction();
    @Child private DispatchNode callNode;
    private final Type resultType;
    protected final TypeUtil.TypeKind kind;

    public ArrayGenerateNode(DispatchNode callNode, Type resultType) {
        this.callNode = callNode;
        this.resultType = resultType;
        this.kind = NoraVmContext.getTypeUtil(null).getKind(resultType.applies[0].type().info);
    }

    public Object evalFunction(Object function, int argument) {
        var args = new Object[2];
        args[0] = function;
        args[1] = argument;
        CompilerAsserts.partialEvaluationConstant(args.length);
        return callNode.executeDispatch(function,args);
    }

    @Specialization(guards = "kind == BOOL")
    protected boolean[] generateBool(int length, Object function){
        var arr = new boolean[length];
        for(int i = 0; i < arr.length; i++){
           arr[i] = (Boolean)evalFunction(function,i);
        }
        return arr;
    }

    @Specialization(guards = "kind == BYTE")
    protected byte[] generateByte(int length, Object function){
        var arr = new byte[length];
        for(int i = 0; i < arr.length; i++){
            arr[i] = (Byte) evalFunction(function,i);
        }
        return arr;
    }

    @Specialization(guards = "kind == INT")
    protected int[] generateInt(int length, Object function){
        var arr = new int[length];
        for(int i = 0; i < arr.length; i++){
            arr[i] = (Integer) evalFunction(function,i);
        }
        return arr;
    }

    @Specialization(guards = "kind == NUM", rewriteOn = UnexpectedResultException.class)
    protected long[] generateLongNum(int length, Object function) throws UnexpectedResultException {
        var arr = new long[length];
        int i = 0;
        Object res = null;
        try {
            for(; i < arr.length; i++){
                res = evalFunction(function,i);
                arr[i] = (Long) res;
            }
            return arr;
        } catch (ClassCastException e){
            transferToInterpreterAndInvalidate();
            var arr2 = new Object[length+1];
            arr2[0] = resultType;
            for(int j = 1; j <= i; j++){
                arr2[j] = arr[j-1];
            }
            arr2[i+1] = res;
            for(int j = i+1; j < arr2.length; j++){
                arr2[j] = evalFunction(function,j-1);
            }
            throw new UnexpectedResultException(arr2);
        }
    }

    @Specialization(guards = "kind == NUM", replaces = "generateLongNum")
    protected Object[] generateBigNum(int length, Object function){
        var arr = new Object[length+1];
        arr[0] = resultType;
        for(int i = 1; i < arr.length; i++){
            arr[i] = evalFunction(function,i-1);
        }
        return arr;
    }

    @Specialization(guards = "isObject(kind)")
    protected Object[] generateObjectArr(int length, Object function){
        var arr = new Object[length+1];
        arr[0] = resultType;
        for(int i = 1; i < arr.length; i++){
            arr[i] = evalFunction(function,i-1);
        }
        return arr;
    }

    @Override
    public Type getType(SpecFrame frame) {
        return resultType;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayGenerateNodeGen.create(callNode.cloneUninitialized(), resultType, getLength().cloneUninitialized(), getFunction().cloneUninitialized());
    }

}
