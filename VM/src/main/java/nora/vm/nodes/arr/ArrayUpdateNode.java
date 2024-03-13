package nora.vm.nodes.arr;

import com.oracle.truffle.api.dsl.*;
import nora.vm.nodes.IntIndexNodeGen;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

import java.math.BigInteger;

//Todo: we need return type to be safe
//      as we can upgrade runtime data
//      say A inherits B
//      update([a1,a2],1,b1) must work and then be of type B
@NodeChild(value = "array", type = NoraNode.class)
@NodeChild(value = "index", type = NoraNode.class)
@NodeChild(value = "update", type = NoraNode.class)
@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
@ReportPolymorphism
public abstract class ArrayUpdateNode extends NoraNode {
    abstract NoraNode getArray();
    //Todo: Update this class : Syntax error so we do not forget
    abstract NoraNode getIndex();
    abstract NoraNode getUpdate();

    private final Type resultType;
    protected final TypeUtil.TypeKind elemKind;

    protected ArrayUpdateNode(Type resultType) {
        this.resultType = resultType;
        this.elemKind = NoraVmContext.getTypeUtil(null).getKind(resultType.applies[0].type().info);
    }

    //covers byte indexes as well
    @Specialization(guards = "elemKind == BOOL")
    public boolean[] formBoolArray(boolean[] arr, int index, boolean value){
        var nArr = arr.clone();
        arr[index] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == BYTE")
    public byte[] formByteArray(byte[] arr, int index, byte value){
        var nArr = arr.clone();
        arr[index] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == INT")
    public int[] formIntArray(int[] arr, int index, int value){
        var nArr = arr.clone();
        arr[index] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == NUM")
    public long[] formLongArray(long[] arr, int index, long value){
        var nArr = arr.clone();
        arr[index] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == NUM")
    public Object[] formLNumArray(long[] arr, int index, BigInteger value){
        var nArr = new Object[arr.length+1];
        nArr[0] = resultType;
        for(int i = 1; i < nArr.length; i++)nArr[i] = arr[i-1];
        nArr[index+1] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == NUM")
    public Object[] formBNumArray(Object[] arr, int index, long value){
        var nArr = arr.clone();
        arr[index+1] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == NUM")
    public Object[] formNumArray(Object[] arr, int index, BigInteger value){
        var nArr = arr.clone();
        arr[index+1] = value;
        return nArr;
    }

    //Can happen in primitive hierarchies
    @Specialization(guards = "elemKind == DATA")
    public Object[] formBoolDataArray(boolean[] arr, int index, Object value){
        var nArr = new Object[arr.length+1];
        nArr[0] = resultType;
        for(int i = 1; i < nArr.length; i++) nArr[i] = arr[i-1];
        nArr[index+1] = value;
        return nArr;
    }
    @Specialization(guards = "elemKind == DATA")
    public Object[] formByteDataArray(byte[] arr, int index, Object value){
        var nArr = new Object[arr.length+1];
        nArr[0] = resultType;
        for(int i = 1; i < nArr.length; i++) nArr[i] = arr[i-1];
        nArr[index+1] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] formIntDataArray(int[] arr, int index, Object value){
        var nArr = new Object[arr.length+1];
        nArr[0] = resultType;
        for(int i = 1; i < nArr.length; i++) nArr[i] = arr[i-1];
        nArr[index+1] = value;
        return nArr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] formNumDataArray(long[] arr, int index, Object value){
        var nArr = new Object[arr.length+1];
        nArr[0] = resultType;
        for(int i = 1; i < nArr.length; i++) nArr[i] = arr[i-1];
        nArr[index+1] = value;
        return nArr;
    }

    @Specialization(guards = "isObject(elemKind)")
    public Object formObjectArray(Object[] arr, int index, Object value){
        var nArr = arr.clone();
        arr[index+1] = value;
        return nArr;
    }

    private Object setGeneric(Object arr, int index, Object value){
        switch (elemKind){
            case BOOL: return formBoolArray((boolean[]) arr,index, (boolean) value);
            case BYTE: return formByteArray((byte[]) arr,index, (byte) value);
            case INT: return formIntArray((int[]) arr,index, (int) value);
            case NUM:
                if(arr instanceof long[] a && value instanceof Long v) return formLongArray(a,index,v);
                if(arr instanceof long[] a && value instanceof BigInteger v) return formLNumArray(a,index,v);
                if(arr instanceof Object[] a && value instanceof BigInteger v) return formNumArray(a,index,v);
                if(arr instanceof Object[] a && value instanceof Long v) return formBNumArray(a,index,v);
                throw new RuntimeException("Type Mismatch");
            case DATA:
                if(arr instanceof boolean[] a) return formBoolDataArray(a,index,value);
                if(arr instanceof byte[] a) return formByteDataArray(a,index,value);
                if(arr instanceof int[] a) return formIntDataArray(a,index,value);
                if(arr instanceof long[] a) return formNumDataArray(a,index,value);
                if(arr instanceof Object[] a) return formObjectArray(a,index,value);
            case STRING:
            case FUNCTION:
            case ARRAY:
            case TYPE:
                assert arr instanceof Object[];
                return formObjectArray((Object[])arr,index,value);
        }

        throw new RuntimeException("Type Missmatch");
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var nArr = getArray().specialise(frame);
        var nIndex = IntIndexNodeGen.create(getIndex()).specialise(frame);
        var nValue = getUpdate().specialise(frame);
        if(nArr instanceof ConstNode ac && nIndex instanceof ConstNode ic && nValue instanceof ConstNode vc){
            return ConstNode.create(setGeneric(ac.execute(null), ic.executeInt(null), vc.execute(null)));
        }
        if(nArr instanceof CachedNode ac && nIndex instanceof CachedNode ic && nValue instanceof CachedNode vc){
            return new CacheNode(ArrayUpdateNodeGen.create(resultType,ac.liftCache(),ic.liftCache(), vc.liftCache()));
        }
        return ArrayUpdateNodeGen.create(resultType,nArr,nIndex, nValue);
    }

    @Override
    public Type getType(SpecFrame frame) {
        return resultType;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayUpdateNodeGen.create(resultType,getArray().cloneUninitialized(), getIndex().cloneUninitialized(), getUpdate().cloneUninitialized());
    }

    @Override
    public int complexity() {
        return 1+getIndex().complexity()+getArray().complexity();
    }
}
