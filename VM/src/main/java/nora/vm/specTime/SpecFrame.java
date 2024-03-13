package nora.vm.specTime;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlotKind;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

import java.util.function.Function;

public class SpecFrame {
    final Object[] valStack;
    Type[] typStack;
    final boolean[] isValCached;
    final Type[] generics;
    final Type[] captures;
    final Type.TypeParameter[] sig;
    final Type self;

    boolean isTrivial = true;

    public SpecFrame(int stackSize, Type.TypeParameter[] sig,  Type[] generics, Type[] captures) {
        this.valStack = new Object[stackSize];
        this.typStack = new Type[stackSize];
        this.isValCached = new boolean[stackSize];
        this.generics = generics;
        this.captures = captures;
        this.sig = sig;
        //Todo: Add Contra, except Last add co
        this.self = new Type(NoraVmContext.getTypeUtil(null).FunctionTypeInfo, sig);
    }

    public SpecFrame(int stackSize, Type.TypeParameter[] sig, Type[] generics) {
        this(stackSize, sig, generics, Type.NO_GENERICS);
    }

    public boolean isLocalKnownVal(int slot) {
        if(slot < valStack.length) return valStack[slot] != null;
        return false;
    }
    public boolean isLocalKnownTyp(int slot) {
        return typStack[slot] != null;
    }

    public Object getValue(int slot){
        return valStack[slot];
    }
    public Type getTyp(int slot){
        return typStack[slot];
    }

    public void setValue(int slot, Object value){
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        valStack[slot] = value;
        typStack[slot] = util.extractFullType(value);
    }

    public void setType(int slot, Type value){
        typStack[slot] = value;
    }

    public <T> T withTypeSnapshot(Function<SpecFrame,T> f){
        var old = typStack.clone();
        var res = f.apply(this);
        typStack = old;
        return res;
    }

    public void markValCachable(int slot) {
        isValCached[slot] = true;
    }

    public boolean isValCached(int slot) {
        return isValCached[slot];
    }


    public Type[] getGenerics(){
        return generics;
    }

    public boolean isKnownArgumentVal(int slot){
        return false;
    }
    public Object getArgumentVal(int slot){
        return null;
    }

    //Note: -1 is becasue we do not have the method/closure in the slot
    public boolean isKnownArgumentTyp(int slot){
        if(slot == 0) return self != null;
        return sig[slot-1] != null;
    }

    public Type getArgumentTyp(int slot){
        if(slot == 0) return self;
        return sig[slot-1].type();
    }

    public boolean hasRetType() {return sig[sig.length-1] != null;}
    public Type getRetType() {return sig[sig.length-1].type();}

    public Type[] getCaptureTypes(){
        return captures;
    }

    public void markNonTrivial(){
        isTrivial = false;
    }

    //Todo: is it worth it
    public boolean isTrivial(NoraNode node) {
        return isTrivial && node.complexity() <= 10; //Make configurable
        //return false;
    }

    public FrameDescriptor buildDescriptor() {
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        var builder = FrameDescriptor.newBuilder(typStack.length);
        for (Type typ : typStack) {
            //null type is no longer possible and instead is used as signal for special static slots
            if (typ == null) {
                builder.addSlot(FrameSlotKind.Static, null, null);
            } else if (typ.info == util.BooleanTypeInfo) {
                builder.addSlot(FrameSlotKind.Boolean, null, null);
            } else if (typ.info == util.IntTypeInfo) {
                builder.addSlot(FrameSlotKind.Int, null, null);
            } else if (typ.info == util.NumTypeInfo) {
                //Be optimistic
                builder.addSlot(FrameSlotKind.Long, null, null);
            } else {
                builder.addSlot(FrameSlotKind.Object, null, null);
            }
        }
        return builder.build();
    }
}

