package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.dsl.ImportStatic;
import com.oracle.truffle.api.dsl.ReportPolymorphism.Megamorphic;
import com.oracle.truffle.api.dsl.NodeField;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

import static nora.vm.types.TypeUtil.isObject;

@ImportStatic({TypeUtil.TypeKind.class,TypeUtil.class})
@NodeField(name = "slot", type = int.class)
@NodeField(name = "slotKind", type = TypeUtil.TypeKind.class)
public abstract class ValNode extends NoraNode{
    abstract int getSlot();
    abstract TypeUtil.TypeKind getSlotKind();

    @Specialization(guards = {"slotKind == DATA", "frame.isObject(getSlot())" })
    protected Object readData(VirtualFrame frame) {
        return frame.getObject(getSlot());
    }

    //Because of the data hierarchies with prims we can no longer be static
    @Specialization(guards = "isPrimitiveSlot(frame,BYTE)")
    protected int readByte(VirtualFrame frame) {
        return frame.getByte(getSlot());
    }

    @Specialization(guards = "isPrimitiveSlot(frame,INT)")
    protected int readInt(VirtualFrame frame) {
        return frame.getInt(getSlot());
    }

    @Specialization(guards = "isPrimitiveSlot(frame,BOOL)")
    protected boolean readBoolean(VirtualFrame frame) {
        return frame.getBoolean(getSlot());
    }

    @Specialization(guards = "isPrimitiveSlot(frame,NUM)", rewriteOn = FrameSlotTypeException.class)
    protected long readLong(VirtualFrame frame) throws FrameSlotTypeException {
        return frame.getLong(getSlot());
    }

    @Megamorphic
    @Specialization(replaces = "readLong",guards = "slotKind == NUM", rewriteOn = {FrameSlotTypeException.class, ClassCastException.class})
    protected BigInteger readBigInteger(VirtualFrame frame) throws FrameSlotTypeException {
        return (BigInteger)frame.getObject(getSlot());
    }

    @Specialization(rewriteOn = FrameSlotTypeException.class, guards = "isObject(slotKind)")
    protected Object readObject(VirtualFrame frame) throws FrameSlotTypeException {
        return frame.getObject(getSlot());
    }

    protected boolean isPrimitiveSlot(VirtualFrame frame, TypeUtil.TypeKind target){
        if(getSlotKind() == target) return true;
        if(getSlotKind() != TypeUtil.TypeKind.DATA) return false;
        return switch (target){
            case BOOL -> frame.isBoolean(getSlot());
            case BYTE -> frame.isByte(getSlot());
            case INT -> frame.isInt(getSlot());
            case NUM -> frame.isLong(getSlot());
            case STRING -> false;
            case FUNCTION -> false;
            case ARRAY -> false;
            case DATA -> true;
            case TYPE -> false;
        };
    }

    @Megamorphic
    @Specialization
    protected Object readValue(VirtualFrame frame) {
        return frame.getValue(getSlot());
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        if(frame.isLocalKnownVal(getSlot())) {
            var constVal = frame.getValue(getSlot());
            return ConstNode.create(constVal);
        } else {
            return ValNodeGen.create(getSlot(), getSlotKind());
        }
    }

    @Override
    public String toString() {
        return "Val["+getSlotKind()+"]("+getSlot()+")";
    }

    @Override
    public ValNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return ValNodeGen.create(getSlot(), getSlotKind());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        assert frame.isLocalKnownTyp(getSlot());
        return frame.getTyp(getSlot());
    }


}
