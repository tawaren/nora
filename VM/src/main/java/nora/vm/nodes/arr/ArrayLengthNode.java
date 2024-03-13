package nora.vm.nodes.arr;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;


@NodeChild(value = "array", type = NoraNode.class)
@ReportPolymorphism
public abstract class ArrayLengthNode extends NoraNode {
    abstract NoraNode getArray();

    //covers bytes as well
    @Specialization
    public int ofBoolArray(boolean[] arr){
        return arr.length;
    }

    @Specialization
    public int ofByteArray(byte[] arr){
        return arr.length;
    }

    @Specialization
    public int ofIntArray(int[] arr){
        return arr.length;
    }

    @Specialization
    public int ofLongArray(long[] arr){
        return arr.length;
    }

    @Specialization
    public int ofObjectArray(Object[] arr){
        return arr.length-1;
    }


    private int ofGeneric(Object arr){
        if(arr instanceof boolean[] a) return a.length;
        if(arr instanceof byte[] a) return a.length;
        if(arr instanceof int[] a) return a.length;
        if(arr instanceof long[] a) return a.length;
        if(arr instanceof Object[] a) return a.length-1;
        throw new RuntimeException("Type Missmatch");
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var nArr = getArray().specialise(frame);
        if(nArr instanceof ConstNode ac){
            return ConstNode.create(ofGeneric(ac.execute(null)));
        }
        if(nArr instanceof CachedNode ac){
            return new CacheNode(ArrayLengthNodeGen.create(ac.liftCache()));
        }
        return ArrayLengthNodeGen.create(nArr);
    }

    @Override
    public Type getType(SpecFrame frame) {
        return NoraVmContext.getTypeUtil(null).IntType;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayLengthNodeGen.create(getArray().cloneUninitialized());
    }

    @Override
    public int complexity() {
        return 1+getArray().complexity();
    }
}
