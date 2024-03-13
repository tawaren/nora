package nora.vm.nodes.arr;

import com.oracle.truffle.api.dsl.*;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

import java.math.BigInteger;


@NodeChild(value = "left", type = NoraNode.class)
@NodeChild(value = "right", type = NoraNode.class)
@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
@ReportPolymorphism
public abstract class ArrayComposeNode extends NoraNode {
    abstract NoraNode getLeft();
    abstract NoraNode getRight();

    private final Type resultType;
    protected final TypeUtil.TypeKind elemKind;

    protected ArrayComposeNode(Type resultType) {
        this.resultType = resultType;
        this.elemKind = NoraVmContext.getTypeUtil(null).getKind(resultType.applies[0].type().info);
    }


    @Specialization(guards = "elemKind == BOOL")
    public boolean[] concatBool(boolean[] left, boolean[] right){
        var arr = new boolean[left.length+right.length];
        System.arraycopy(left, 0, arr, 0, left.length);
        System.arraycopy(right, 0, arr, left.length, right.length);
        return arr;
    }

    @Specialization(guards = "elemKind == BYTE")
    public byte[] concatByte(byte[] left, byte[] right){
        var arr = new byte[left.length+right.length];
        System.arraycopy(left, 0, arr, 0, left.length);
        System.arraycopy(right, 0, arr, left.length, right.length);
        return arr;
    }

    @Specialization(guards = "elemKind == INT")
    public int[] concatInt(int[] left, int[] right){
        var arr = new int[left.length+right.length];
        System.arraycopy(left, 0, arr, 0, left.length);
        System.arraycopy(right, 0, arr, left.length, right.length);
        return arr;
    }

    @Specialization(guards = "elemKind == NUM")
    public long[] concatLong(long[] left, long[] right){
        var arr = new long[left.length+right.length];
        System.arraycopy(left, 0, arr, 0, left.length);
        System.arraycopy(right, 0, arr, left.length, right.length);
        return arr;
    }

    @Specialization(guards = "elemKind == NUM")
    public Object[] concatBig(Object[] left, Object[] right){
        var arr = new Object[left.length+right.length-1];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 1, left.length-1);
        System.arraycopy(right, 1, arr, left.length, right.length-1);
        return arr;
    }

    @Specialization(guards = "elemKind == NUM")
    public Object[] concatBigLong(Object[] left, long[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 0, left.length);
        var offset = left.length;
        for(int i = 0; i < right.length; i++)arr[i+offset] = BigInteger.valueOf(right[i]);
        return arr;
    }

    @Specialization(guards = "elemKind == NUM")
    public Object[] concatLongBig(long[] left, Object[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        for(int i = 0; i < left.length; i++)arr[i+1] = BigInteger.valueOf(left[i]);
        System.arraycopy(right, 1, arr, left.length+1, right.length-1);
        return arr;
    }

    @Specialization(guards = "isObject(elemKind)")
    public Object[] concatObject(Object[] left, Object[] right){
        assert left[0].equals(right[0]);
        var arr = new Object[left.length+right.length-1];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 1, left.length-1);
        System.arraycopy(right, 1, arr, left.length, right.length-1);
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatObjectBool(Object[] left, boolean[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 0, left.length);
        var offset = left.length;
        for(int i = 0; i < right.length; i++)arr[i+offset] = right[i];
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatBoolObject(boolean[] left, Object[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        for(int i = 0; i < left.length; i++)arr[i+1] = left[i];
        System.arraycopy(right, 1, arr, left.length+1, right.length-1);
        return arr;
    }
    @Specialization(guards = "elemKind == DATA")
    public Object[] concatObjectByte(Object[] left, byte[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 0, left.length);
        var offset = left.length;
        for(int i = 0; i < right.length; i++)arr[i+offset] = right[i];
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatByteObject(byte[] left, Object[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        for(int i = 0; i < left.length; i++)arr[i+1] = left[i];
        System.arraycopy(right, 1, arr, left.length+1, right.length-1);
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatObjectInt(Object[] left, int[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 0, left.length);
        var offset = left.length;
        for(int i = 0; i < right.length; i++)arr[i+offset] = right[i];
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatIntObject(int[] left, Object[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        for(int i = 0; i < left.length; i++)arr[i+1] = left[i];
        System.arraycopy(right, 1, arr, left.length+1, right.length-1);
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatObjectLong(Object[] left, long[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        System.arraycopy(left, 1, arr, 0, left.length);
        var offset = left.length;
        for(int i = 0; i < right.length; i++)arr[i+offset] = right[i];
        return arr;
    }

    @Specialization(guards = "elemKind == DATA")
    public Object[] concatLongObject(long[] left, Object[] right){
        var arr = new Object[left.length+right.length];
        arr[0] = resultType;
        for(int i = 0; i < left.length; i++)arr[i+1] = left[i];
        System.arraycopy(right, 1, arr, left.length+1, right.length-1);
        return arr;
    }

    private Object composeGeneric(Object left, Object right){
        switch (elemKind){
            case BOOL: return concatBool((boolean[]) left,(boolean[]) right);
            case BYTE: return concatByte((byte[]) left,(byte[]) right);
            case INT: return concatInt((int[]) left,(int[]) right);
            case NUM:
                if(left instanceof long[] ll && right instanceof long[] rl) return concatLong(ll,rl);
                if(left instanceof Object[] lb && right instanceof long[] rl) return concatBigLong(lb,rl);
                if(left instanceof long[] ll && right instanceof Object[] rb) return concatLongBig(ll,rb);
                if(left instanceof Object[] lb && right instanceof Object[] rb) return concatBig(lb,rb);
                throw new RuntimeException("Type Mismatch");
            case DATA:
                if(left instanceof Object[] ol && right instanceof Object[] or) return concatObject(ol,or);
                if(left instanceof Object[] ob && right instanceof byte[] bl) return concatObjectByte(ob,bl);
                if(left instanceof byte[] bl && right instanceof Object[] ob) return concatByteObject(bl,ob);
                if(left instanceof Object[] ob && right instanceof boolean[] bl) return concatObjectBool(ob,bl);
                if(left instanceof boolean[] bl && right instanceof Object[] ob) return concatBoolObject(bl,ob);
                if(left instanceof Object[] ob && right instanceof int[] il) return concatObjectInt(ob,il);
                if(left instanceof int[] il && right instanceof Object[] ob) return concatIntObject(il,ob);
                if(left instanceof Object[] ob && right instanceof long[] rl) return concatObjectLong(ob,rl);
                if(left instanceof long[] ll && right instanceof Object[] ob) return concatLongObject(ll,ob);
            case STRING:
            case FUNCTION:
            case ARRAY:
            case TYPE:
                assert left instanceof Object[];
                assert right instanceof Object[];
                return concatObject((Object[])left,(Object[])right);
        }

        throw new RuntimeException("Type Missmatch");
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var nLeft = getLeft().specialise(frame);
        var nRight = getRight().specialise(frame);
        if(nLeft instanceof ConstNode lc && nRight instanceof ConstNode rc){
            return ConstNode.create(composeGeneric(lc, rc));
        }
        if(nLeft instanceof CachedNode lc && nRight instanceof CachedNode rc){
            return new CacheNode(ArrayComposeNodeGen.create(resultType, lc.liftCache(),rc.liftCache()));
        }
        return ArrayComposeNodeGen.create(resultType,nLeft,nRight);
    }

    @Override
    public Type getType(SpecFrame frame) {
        return getLeft().getType(frame);
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayComposeNodeGen.create(resultType,getLeft().cloneUninitialized(), getRight().cloneUninitialized());
    }

    @Override
    public int complexity() {
        return 1+getLeft().complexity()+getRight().complexity();
    }
}
