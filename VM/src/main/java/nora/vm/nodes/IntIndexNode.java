package nora.vm.nodes;

import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "index", type = NoraNode.class)
public abstract class IntIndexNode extends NoraNode {

    public abstract NoraNode getIndex();

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        try {
            return executeInt(virtualFrame);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    //will cover bytes as well thanks to implicit casts
    @Specialization
    public int fromInt(int index){
        return index;
    }

    @Specialization
    public int fromLong(long index){
        int i = (int) index;
        if(i != index) throw new IndexOutOfBoundsException();
        return i;
    }

    @Specialization
    public int fromBig(BigInteger index){
        try {
            return index.intValueExact();
        } catch (ArithmeticException e){
            throw new IndexOutOfBoundsException();
        }
    }

    private int fromGeneric(Object index){
        if(index instanceof Byte b) return fromInt(b);
        if(index instanceof Integer i) return fromInt(i);
        if(index instanceof Long l) return fromLong(l);
        if(index instanceof BigInteger b) return fromBig(b);
        throw new RuntimeException("Type Mismatch");
    }

    public NoraNode specialise(SpecFrame frame) throws Exception {
        var util = NoraVmContext.getTypeUtil(null);
        var nIndex = getIndex().specialise(frame);
        if(nIndex.getType(frame).info == util.IntTypeInfo) return nIndex;
        if(nIndex instanceof ConstNode cn) {
            return ConstNode.create(fromGeneric(cn.execute(null)));
        }
        if(nIndex instanceof CachedNode cn) {
            return new CacheNode(IntIndexNodeGen.create(cn.liftCache()));
        }
        return IntIndexNodeGen.create(getIndex().specialise(frame));
    }

    @Override
    public NoraNode cloneUninitialized() {
        return IntIndexNodeGen.create(getIndex().cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        return NoraVmContext.getTypeUtil(null).IntType;
    }
}
