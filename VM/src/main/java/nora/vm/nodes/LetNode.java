package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "expr", type = NoraNode.class)
public abstract class LetNode extends StmNode{
    public final int slot;
    abstract NoraNode getExpr();

    protected LetNode(int slot) {
        this.slot = slot;
    }

    //Because of the data hierarchies with prims we can no longer be static
    @Specialization
    protected Void writeByte(VirtualFrame virtualFrame, byte value) {
        virtualFrame.setByte(slot, value);
        return null;
    }

    @Specialization
    protected Void writeInt(VirtualFrame virtualFrame, int value) {
        virtualFrame.setInt(slot, value);
        return null;
    }

    @Specialization
    protected Void writeLong(VirtualFrame virtualFrame, long value) {
        virtualFrame.setLong(slot, value);
        return null;
    }

    @Specialization
    protected Void writeBoolean(VirtualFrame virtualFrame, boolean value) {
        virtualFrame.setBoolean(slot, value);
        return null;
    }

    @Specialization
    protected Void writeBigInteger(VirtualFrame virtualFrame, BigInteger value) {
        virtualFrame.setObject(slot, value);
        return null;
    }

    @Specialization
    protected Void writeObject(VirtualFrame virtualFrame, Object value) {
        virtualFrame.setObject(slot, value);
        return null;
    }

    @Override
    public StmNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var res = getExpr().specialise(frame);
        if(res instanceof ConstNode){
            frame.setValue(slot, res.execute(null));
            return null;
        }
        if(!frame.isLocalKnownTyp(slot)){
            //This looks like it double checks get type (together with getType)
            //  However, spec sets type so we never get into this if getType called first
            var typ = res.getType(frame);
            assert typ != null;
            frame.setType(slot, typ);
        }
        if(res instanceof CachedNode) frame.markValCachable(slot);
        return LetNodeGen.create(slot, res);

    }

    @Override
    public String toString() {
        return "Val("+slot+") = "+getExpr();
    }

    @Override
    public LetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return LetNodeGen.create(slot, getExpr().cloneUninitialized());
    }


    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        if(!frame.isLocalKnownTyp(slot)){
            //This looks like it double checks get type (together with spec)
            //  However, spec sets type so we never get into this if if spec called first
            var t = getExpr().getType(frame);
            if(t != null){
                frame.setType(slot, t);
            }
        }
        return null;
    }
}
