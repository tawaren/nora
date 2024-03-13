package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.TypeUtil;

@ImportStatic(TypeUtil.TypeKind.class)
@NodeField(name = "slot", type = int.class)
@NodeChild(value = "arg", type = NoraNode.class)
public abstract class DelayedArgSetterNode extends Node {
    public abstract int getSlot();
    public abstract NoraNode getArg();

    @Child public DelayedArgSetterNode next;

    public void setNext(DelayedArgSetterNode next) {
        this.next = super.insert(next);
    }

    public abstract void execute(VirtualFrame frame);

    @Specialization(guards = "frame.isBoolean(slot)")
    public void setBool(VirtualFrame frame, boolean val) {
        if(next != null) next.execute(frame);
        frame.setBoolean(getSlot(),val);
    }

    @Specialization(guards = "frame.isByte(slot)")
    public void setByte(VirtualFrame frame, byte val) {
        if(next != null) next.execute(frame);
        frame.setByte(getSlot(),val);
    }

    @Specialization(guards = "frame.isInt(slot)")
    public void setInt(VirtualFrame frame, int val) {
        if(next != null) next.execute(frame);
        frame.setInt(getSlot(),val);
    }

    @Specialization(guards = "frame.isLong(slot)")
    public void setLong(VirtualFrame frame, long val) {
        if(next != null) next.execute(frame);
        frame.setLong(getSlot(),val);
    }

    @Specialization
    public void setObject(VirtualFrame frame, Object val) {
        if(next != null) next.execute(frame);
        frame.setObject(getSlot(),val);
    }

    public DelayedArgSetterNode specialise(SpecFrame frame) throws Exception {
        var res = DelayedArgSetterNodeGen.create(getArg().specialise(frame), getSlot());
        if(next != null) res.setNext(next.specialise(frame));
        return res;
    }

    public DelayedArgSetterNode cloneUninitialized(){
        var res = DelayedArgSetterNodeGen.create(getArg().cloneUninitialized(), getSlot());
        if(next != null) res.setNext(next.cloneUninitialized());
        return res;
    }

    public int complexity() {
        var count = 1+getArg().complexity();
        if(next != null) count+=next.complexity();
        return count;
    }
}
