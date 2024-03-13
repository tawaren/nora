package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.Node;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.TypeUtil;

@ImportStatic(TypeUtil.TypeKind.class)
@NodeField(name = "slot", type = int.class)
@NodeChild(value = "arg", type = NoraNode.class)
public abstract class ArgSetterNode extends Node {
    public abstract int getSlot();
    public abstract NoraNode getArg();
    public abstract void execute(VirtualFrame frame);

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public void setBool(VirtualFrame frame, boolean val) {
        frame.setBoolean(getSlot(),val);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public void setByte(VirtualFrame frame, byte val) {
        frame.setByte(getSlot(),val);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public void setInt(VirtualFrame frame, int val) {
        frame.setInt(getSlot(),val);
    }

    @Specialization(rewriteOn = {FrameSlotTypeException.class})
    public void setLong(VirtualFrame frame, long val) {
        frame.setLong(getSlot(),val);
    }

    @Specialization()
    public void setObject(VirtualFrame frame, Object val) {
        frame.setObject(getSlot(),val);
    }

    public ArgSetterNode specialise(SpecFrame frame) throws Exception {
        return ArgSetterNodeGen.create(getArg().specialise(frame), getSlot());
    }

    public ArgSetterNode cloneUninitialized(){
        return ArgSetterNodeGen.create(getArg().cloneUninitialized(), getSlot());
    }
}
