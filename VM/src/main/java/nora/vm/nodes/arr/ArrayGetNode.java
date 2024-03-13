package nora.vm.nodes.arr;

import com.oracle.truffle.api.dsl.Executed;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.IntIndexNodeGen;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

@ReportPolymorphism
public abstract class ArrayGetNode extends NoraNode {
    @Executed @Child protected NoraNode array;
    @Executed @Child NoraNode index;

    public ArrayGetNode(NoraNode array, NoraNode index) {
        this.array = array;
        this.index = IntIndexNodeGen.create(index);
    }

    //Covers byte index as well
    @Specialization
    public boolean formBoolArray(boolean[] arr, int index){
        return arr[index];
    }

    @Specialization
    public int formByteArray(byte[] arr, int index){
        return arr[index];
    }

    @Specialization
    public int formIntArray(int[] arr, int index){
        return arr[index];
    }

    @Specialization
    public long formLongArray(long[] arr, int index){
        return arr[index];
    }

    @Specialization
    public Object formObjectArray(Object[] arr, int index){
        return arr[index+1];
    }

    private Object getGeneric(Object arr, int index){
        if(arr instanceof boolean[] a) return a[index];
        if(arr instanceof byte[] a) return a[index];
        if(arr instanceof int[] a) return a[index];
        if(arr instanceof long[] a) return a[index];
        if(arr instanceof Object[] a) return a[index+1];
        throw new RuntimeException("Type Missmatch");
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var nArr = array.specialise(frame);
        //will eliminate the wrapper if not necessary
        var nIndex = IntIndexNodeGen.create(index).specialise(frame);
        //Todo: wrap in ArrayIndexNode if not an int
        if(nArr instanceof ConstNode ac && nIndex instanceof ConstNode ic){
            return ConstNode.create(getGeneric(ac.execute(null), ic.executeInt(null)));
        }
        if(nArr instanceof CachedNode ac && nIndex instanceof CachedNode ic){
            return new CacheNode(ArrayGetNodeGen.create(ac.liftCache(),ic.liftCache()));
        }
        return ArrayGetNodeGen.create(nArr,nIndex);
    }

    @Override
    public Type getType(SpecFrame frame) {
        var arrType = array.getType(frame);
        return arrType.applies[0].type();
    }

    @Override
    public NoraNode cloneUninitialized() {
        return ArrayGetNodeGen.create(array.cloneUninitialized(), index.cloneUninitialized());
    }

    @Override
    public int complexity() {
        return 2+index.complexity()+array.complexity();
    }
}

