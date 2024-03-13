package nora.vm.nodes.consts;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public abstract class ConstNode extends CachedNode {
    abstract Object getConstant();

    int count = 0;
    @Override
    public Object execute(VirtualFrame frame) {
        return getConstant();
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        return this;
    }

    @Override
    public String toString() {
        return getConstant().toString();
    }

    @Override
    public ConstNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return this;
    }

    @Override
    public boolean isUnboxed() {
        //Technically they are Unboxed but just for switching it is not worth it
        return false;
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return NoraVmContext.getTypeUtil(null).extractFullType(getConstant());
    }

    public static ConstNode create(Object res){
        assert res != null;
        if(res instanceof Boolean b) return new BooleanNode(b);
        if(res instanceof Byte b) return new ByteNode(b);
        if(res instanceof Integer i) return new IntNode(i);
        if(res instanceof Long l) return new LongNode(l);
        if(res instanceof Type t) return new TypeNode(t);
        return new ObjectNode(res);
    }

    public static ConstNode create(byte res){
        return new ByteNode(res);
    }

    public static ConstNode create(int res){
        return new IntNode(res);
    }

    public static ConstNode create(boolean res){
        return new BooleanNode(res);
    }

    public static ConstNode create(long res){
        return new LongNode(res);
    }

    public static ConstNode create(Type res){
        return new TypeNode(res);
    }

    @Override
    public NoraNode liftCache() {
        return this;
    }


}
