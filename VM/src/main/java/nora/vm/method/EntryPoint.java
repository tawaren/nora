package nora.vm.method;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.TruffleLanguage;
import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.RootNode;
import nora.vm.nodes.EnsureOrRetNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.type.BooleanSwitchNode;
import nora.vm.nodes.type.IntSwitchNode;
import nora.vm.nodes.type.NumericSwitchNode;
import nora.vm.nodes.type.SwitchNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.specTime.SpecFrame;
import nora.vm.truffle.NoraLanguage;
import nora.vm.types.Type;

public class EntryPoint extends RootNode {
    public final int locals;
    @Child public NoraNode body;
    private final String id;
    private final boolean isTrivial;

    public EntryPoint(String id, TruffleLanguage<?> lang, FrameDescriptor desc, boolean isTrivial, NoraNode body) {
        super(lang, desc);
        this.locals = desc.getNumberOfSlots();
        this.id = id;
        this.isTrivial = isTrivial;
        if(body == null) throw new RuntimeException();
        this.body = body;
    }

    public EntryPoint(int locals, NoraNode body) {
        super(NoraLanguage.get(null));
        this.locals = locals;
        this.isTrivial = false;
        if(body == null) throw new RuntimeException();
        this.body = body;
        this.id = null;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return body.execute(frame);
    }

    private NoraNode primTypedExec(SpecFrame frame) throws Exception {
        var newInner = body.specialise(frame);
        var typ = newInner.getType(frame);
        if(typ == null) typ = frame.getRetType();
        return SwitchNode.safeCreate(typ.info, newInner);
    }

    public EntryPoint specialise(String id, Type.TypeParameter[] sig, Type[] generics) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        try {
            var frame = new SpecFrame(locals, sig, generics);
            var body = primTypedExec(frame);
            //This has no arguments just a return (&and is not already constant)
            if(sig.length == 1 && !(body instanceof CachedNode)){
                body = new CacheNode(body);
            }
            return new EntryPoint(id, getLanguage(NoraLanguage.class), frame.buildDescriptor(), frame.isTrivial(body), body);
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            var body = ConstNode.create(retExp.getValue());
            return new EntryPoint(id, getLanguage(NoraLanguage.class), new FrameDescriptor(), true, body);
        }
    }


    public EntryPoint specialiseClosure(Type.TypeParameter[] sig, Type[] captures, Type[] generics) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        try {
            var frame = new SpecFrame(locals, sig, generics, captures);
            var body = primTypedExec(frame);
            return new EntryPoint("<Closure>",getLanguage(NoraLanguage.class), frame.buildDescriptor(), frame.isTrivial(body), body);
        } catch (EnsureOrRetNode.EarlyReturnException retExp){
            var body = ConstNode.create(retExp.getValue());
            return new EntryPoint("<Closure>", getLanguage(NoraLanguage.class), new FrameDescriptor(), true, body);
        }
    }

    @Override
    protected boolean isTrivial() {
        return isTrivial;
    }

    @Override
    public boolean isCloningAllowed() {
        return true;
    }

    @Override
    protected boolean isCloneUninitializedSupported() {
        return true;
    }

    @Override
    public EntryPoint cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        assert super.getFrameDescriptor() != null;
        return new EntryPoint(id, super.getLanguage(NoraLanguage.class), super.getFrameDescriptor(), isTrivial, body.cloneUninitialized());
    }

    @Override
    public String getName() {
        if(id == null) return "<Unspecialized>";
        return id.split("::")[1];
    }

    @Override
    public String getQualifiedName(){
        return id;
    }

    @Override
    public String toString() {
        return id;
    }

}
