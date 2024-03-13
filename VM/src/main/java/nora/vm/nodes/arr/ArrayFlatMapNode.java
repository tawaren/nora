package nora.vm.nodes.arr;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
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
@NodeChild(value = "array", type = NoraNode.class)
@NodeChild(value = "function", type = NoraNode.class)
@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
public abstract class ArrayFlatMapNode extends ExecutionNode {
    protected abstract NoraNode getArray();
    protected abstract NoraNode getFunction();
    @Child private DispatchNode callNode;
    private final Type resultType;
    protected final TypeUtil.TypeKind kind;

    public ArrayFlatMapNode(DispatchNode callNode, Type resultType) {
        this.callNode = callNode;
        this.resultType = resultType;
        this.kind = NoraVmContext.getTypeUtil(null).getKind(resultType.applies[0].type().info);
    }

    public Object evalFunction(Object function, Object elem) {
        var args = new Object[2];
        args[0] = function;
        args[1] = elem;
        CompilerAsserts.partialEvaluationConstant(args.length);
        return callNode.executeDispatch(function,args);
    }

    //Note: Java primitives lead to an exponential blow up here
    //      To prevent having much of complex code we have simple helper mathods
    //      We use static overloading to keep it clean

    private boolean[] ensureSize(int k, boolean[] arr){
        if(k >= arr.length) {
            var nOutArr = new boolean[arr.length*2];
            System.arraycopy(arr, 0, nOutArr, 0, arr.length);
            arr = nOutArr;
        }
        return arr;
    }

    private byte[] ensureSize(int k, byte[] arr){
        if(k >= arr.length) {
            var nOutArr = new byte[arr.length*2];
            System.arraycopy(arr, 0, nOutArr, 0, arr.length);
            arr = nOutArr;
        }
        return arr;
    }

    private int[] ensureSize(int k, int[] arr){
        if(k >= arr.length) {
            var nOutArr = new int[arr.length*2];
            System.arraycopy(arr, 0, nOutArr, 0, arr.length);
            arr = nOutArr;
        }
        return arr;
    }

    private long[] ensureSize(int k, long[] arr){
        if(k >= arr.length) {
            var nOutArr = new long[arr.length*2];
            System.arraycopy(arr, 0, nOutArr, 0, arr.length);
            arr = nOutArr;
        }
        return arr;
    }

    private Object[] ensureSize(int k, Object[] arr){
        if(k >= arr.length) {
            var nOutArr = new Object[(arr.length*2)-1];
            System.arraycopy(arr, 0, nOutArr, 0, arr.length);
            arr = nOutArr;
        }
        return arr;
    }

    private boolean[] shrink(int k, boolean[] arr){
        var finArr =  new boolean[k];
        System.arraycopy(arr, 0, finArr, 0, finArr.length);
        return finArr;
    }

    private byte[] shrink(int k, byte[] arr){
        var finArr =  new byte[k];
        System.arraycopy(arr, 0, finArr, 0, finArr.length);
        return finArr;
    }

    private int[] shrink(int k, int[] arr){
        var finArr =  new int[k];
        System.arraycopy(arr, 0, finArr, 0, finArr.length);
        return finArr;
    }

    private long[] shrink(int k, long[] arr){
        var finArr =  new long[k];
        System.arraycopy(arr, 0, finArr, 0, finArr.length);
        return finArr;
    }

    private Object[] shrink(int k, Object[] arr){
        var finArr =  new Object[k];
        System.arraycopy(arr, 0, finArr, 0, finArr.length);
        return finArr;
    }

    private int flatten(int k, boolean[] arr, boolean[] res){
        for(int j = 0; j < arr.length; j++){
            arr[k++] = res[j];
        }
        return k;
    }

    private int flatten(int k, byte[] arr, byte[] res){
        for(int j = 0; j < arr.length; j++){
            arr[k++] = res[j];
        }
        return k;
    }

    private int flatten(int k, int[] arr, int[] res){
        for(int j = 0; j < arr.length; j++){
            arr[k++] = res[j];
        }
        return k;
    }

    private int flatten(int k, long[] arr, long[] res){
        for(int j = 0; j < arr.length; j++){
            arr[k++] = res[j];
        }
        return k;
    }

    private int flatten(int k, Object[] arr, Object[] res){
        for(int j = 0; j < arr.length; j++){
            arr[k++] = res[j];
        }
        return k;
    }

    @Specialization(guards = "kind == BOOL")
    protected boolean[] generateBoolBool(boolean[] inArr, Object function){
        var outArr = new boolean[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (boolean[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BOOL")
    protected boolean[] generateByteBool(byte[] inArr, Object function){
        var outArr = new boolean[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (boolean[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BOOL")
    protected boolean[] generateIntBool(int[] inArr, Object function){
        var outArr = new boolean[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (boolean[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BOOL")
    protected boolean[] generateLongBool(long[] inArr, Object function){
        var outArr = new boolean[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (boolean[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BOOL")
    protected boolean[] generateObjectBool(Object[] inArr, Object function){
        var outArr = new boolean[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (boolean[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BYTE")
    protected byte[] generateBoolByte(boolean[] inArr, Object function){
        var outArr = new byte[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (byte[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BYTE")
    protected byte[] generateByteByte(byte[] inArr, Object function){
        var outArr = new byte[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (byte[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BYTE")
    protected byte[] generateIntByte(int[] inArr, Object function){
        var outArr = new byte[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (byte[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BYTE")
    protected byte[] generateLongByte(long[] inArr, Object function){
        var outArr = new byte[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (byte[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == BYTE")
    protected byte[] generateObjectByte(Object[] inArr, Object function){
        var outArr = new byte[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (byte[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == INT")
    protected int[] generateBoolInt(boolean[] inArr, Object function){
        var outArr = new int[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (int[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == INT")
    protected int[] generateByteInt(byte[] inArr, Object function){
        var outArr = new int[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (int[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == INT")
    protected int[] generateIntInt(int[] inArr, Object function){
        var outArr = new int[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (int[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == INT")
    protected int[] generateLongInt(long[] inArr, Object function){
        var outArr = new int[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (int[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "kind == INT")
    protected int[] generateObjectInt(Object[] inArr, Object function){
        var outArr = new int[inArr.length*2];
        var k = 0;
        for(int i = 0; i < inArr.length; i++){
            var res = (int[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    //This runs in interpreter once on despec
    // So we can afford performance penalty
    @FunctionalInterface
    private interface GetAsObject{
        Object get(int index);
    }

    private int revertToObjectArr(int k, Object res, Object[] outArr, GetAsObject f){
        outArr[0] = resultType;
        k++;
        for(int y = 1; y < k; y++) outArr[y] = f.get(y-1);
        var resArr = (Object[])res;
        for(int j = 0; j < resArr.length; j++){
            if(k >= outArr.length) {
                var nOutArr = new Object[outArr.length*2];
                System.arraycopy(outArr, 0, nOutArr, 0, outArr.length);
                outArr = nOutArr;
            }
            outArr[k++] = resArr[j];
        }
        return k;
    }

    @Specialization(guards = "kind == NUM", rewriteOn = UnexpectedResultException.class)
    protected long[] generateBoolNum(boolean[] inArr, Object function) throws UnexpectedResultException {
        var outArr = new long[inArr.length*2];
        var k = 0;
        int i = 0;
        Object res = null;
        try {
            for(; i < inArr.length; i++){
                res = evalFunction(function,inArr[i]);
                var resArr = (long[])res;
                outArr = ensureSize(k+resArr.length, outArr);
                k = flatten(k,outArr,resArr);
            }
            return shrink(k, outArr);
        } catch (ClassCastException e){
            transferToInterpreterAndInvalidate();
            final var fArr = outArr;
            var nOutArr = new Object[outArr.length+1];
            k = revertToObjectArr(k, res, nOutArr, (index) -> fArr[index]);
            for(; i < inArr.length; i++){
                var nResArr = (Object[])evalFunction(function,inArr[i]);
                nOutArr = ensureSize(k+nResArr.length, nOutArr);
                k = flatten(k,nOutArr,nResArr);
            }
            throw new UnexpectedResultException(shrink(k, nOutArr));
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = UnexpectedResultException.class)
    protected long[] generateByteNum(byte[] inArr, Object function) throws UnexpectedResultException {
        var outArr = new long[inArr.length*2];
        var k = 0;
        int i = 0;
        Object res = null;
        try {
            for(; i < inArr.length; i++){
                res = evalFunction(function,inArr[i]);
                var resArr = (long[])res;
                outArr = ensureSize(k+resArr.length, outArr);
                k = flatten(k,outArr,resArr);
            }
            return shrink(k, outArr);
        } catch (ClassCastException e){
            transferToInterpreterAndInvalidate();
            final var fArr = outArr;
            var nOutArr = new Object[outArr.length+1];
            k = revertToObjectArr(k, res, nOutArr, (index) -> fArr[index]);
            for(; i < inArr.length; i++){
                var nResArr = (Object[])evalFunction(function,inArr[i]);
                nOutArr = ensureSize(k+nResArr.length, nOutArr);
                k = flatten(k,nOutArr,nResArr);
            }
            throw new UnexpectedResultException(shrink(k, nOutArr));
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = UnexpectedResultException.class)
    protected long[] generateIntNum(int[] inArr, Object function) throws UnexpectedResultException {
        var outArr = new long[inArr.length*2];
        var k = 0;
        int i = 0;
        Object res = null;
        try {
            for(; i < inArr.length; i++){
                res = evalFunction(function,inArr[i]);
                var resArr = (long[])res;
                outArr = ensureSize(k+resArr.length, outArr);
                k = flatten(k,outArr,resArr);
            }
            return shrink(k, outArr);
        } catch (ClassCastException e){
            transferToInterpreterAndInvalidate();
            final var fArr = outArr;
            var nOutArr = new Object[outArr.length+1];
            k = revertToObjectArr(k, res, nOutArr, (index) -> fArr[index]);
            for(; i < inArr.length; i++){
                var nResArr = (Object[])evalFunction(function,inArr[i]);
                nOutArr = ensureSize(k+nResArr.length, nOutArr);
                k = flatten(k,nOutArr,nResArr);
            }
            throw new UnexpectedResultException(shrink(k, nOutArr));
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = UnexpectedResultException.class)
    protected long[] generateNumNum(long[] inArr, Object function) throws UnexpectedResultException {
        var outArr = new long[inArr.length*2];
        var k = 0;
        int i = 0;
        Object res = null;
        try {
            for(; i < inArr.length; i++){
                res = evalFunction(function,inArr[i]);
                var resArr = (long[])res;
                outArr = ensureSize(k+resArr.length, outArr);
                k = flatten(k,outArr,resArr);
            }
            return shrink(k, outArr);
        } catch (ClassCastException e){
            transferToInterpreterAndInvalidate();
            final var fArr = outArr;
            var nOutArr = new Object[outArr.length+1];
            k = revertToObjectArr(k, res, nOutArr, (index) -> fArr[index]);
            for(; i < inArr.length; i++){
                var nResArr = (Object[])evalFunction(function,inArr[i]);
                nOutArr = ensureSize(k+nResArr.length, nOutArr);
                k = flatten(k,nOutArr,nResArr);
            }
            throw new UnexpectedResultException(shrink(k, nOutArr));
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = UnexpectedResultException.class)
    protected long[] generateObjectNum(Object[] inArr, Object function) throws UnexpectedResultException {
        var outArr = new long[inArr.length*2];
        var k = 0;
        int i = 1;
        Object res = null;
        try {
            for(; i < inArr.length; i++){
                res = evalFunction(function,inArr[i]);
                var resArr = (long[])res;
                outArr = ensureSize(k+resArr.length, outArr);
                k = flatten(k,outArr,resArr);
            }
            return shrink(k, outArr);
        } catch (ClassCastException e){
            transferToInterpreterAndInvalidate();
            final var fArr = outArr;
            var nOutArr = new Object[outArr.length+1];
            k = revertToObjectArr(k, res, nOutArr, (index) -> fArr[index]);
            for(; i < inArr.length; i++){
                var nResArr = (Object[])evalFunction(function,inArr[i]);
                nOutArr = ensureSize(k+nResArr.length, nOutArr);
                k = flatten(k,nOutArr,nResArr);
            }
            throw new UnexpectedResultException(shrink(k, nOutArr));
        }
    }

    @Specialization(guards = "isObject(kind)")
    protected Object[] generateBoolObject(boolean[] inArr, Object function){
        var outArr = new Object[(inArr.length*2)+1];
        outArr[0] = resultType;
        var k = 1;
        for(int i = 0; i < inArr.length; i++){
            var res = (Object[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "isObject(kind)")
    protected Object[] generateByteObject(byte[] inArr, Object function){
        var outArr = new Object[(inArr.length*2)+1];
        outArr[0] = resultType;
        var k = 1;
        for(int i = 0; i < inArr.length; i++){
            var res = (Object[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "isObject(kind)")
    protected Object[] generateIntObject(int[] inArr, Object function){
        var outArr = new Object[(inArr.length*2)+1];
        outArr[0] = resultType;
        var k = 1;
        for(int i = 0; i < inArr.length; i++){
            var res = (Object[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "isObject(kind)")
    protected Object[] generateLongObject(long[] inArr, Object function){
        var outArr = new Object[(inArr.length*2)+1];
        outArr[0] = resultType;
        var k = 1;
        for(int i = 0; i < inArr.length; i++){
            var res = (Object[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Specialization(guards = "isObject(kind)")
    protected Object[] generateObjectObject(Object[] inArr, Object function){
        var outArr = new Object[(inArr.length*2)+1];
        outArr[0] = resultType;
        var k = 1;
        for(int i = 0; i < inArr.length; i++){
            var res = (Object[])evalFunction(function,inArr[i]);
            outArr = ensureSize(k+res.length, outArr);
            k = flatten(k,outArr,res);
        }
        return shrink(k, outArr);
    }

    @Override
    public Type getType(SpecFrame frame) {
        return resultType;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayFlatMapNodeGen.create(callNode.cloneUninitialized(), resultType, getArray().cloneUninitialized(), getFunction().cloneUninitialized());
    }

}
