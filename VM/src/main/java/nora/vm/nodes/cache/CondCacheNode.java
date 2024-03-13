package nora.vm.nodes.cache;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import nora.vm.nodes.ForwardNode;
import nora.vm.nodes.IfNode;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class CondCacheNode extends CachedNode {
    @Child private NoraNode cond;
    @Child private NoraNode then;
    @Child private NoraNode other;
    private final Type staticRetType;

    public CondCacheNode(NoraNode cond, NoraNode then, NoraNode other, Type staticRetType) {
        this.cond = cond;
        this.then = then;
        this.other = other;
        this.staticRetType = staticRetType;
    }

    private NoraNode core(VirtualFrame frame) {
        try {
            var res = cond.executeBoolean(frame);
            if(res){
                return replace(then);
            } else {
                return replace(other);
            }
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            //We can not recover from this
            throw new IllegalArgumentException("If's are only defined on booleans");
        }

    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        return core(virtualFrame).execute(virtualFrame);
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return core(virtualFrame).executeBoolean(virtualFrame);
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return core(virtualFrame).executeByte(virtualFrame);
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return core(virtualFrame).executeInt(virtualFrame);
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        return core(virtualFrame).executeLong(virtualFrame);
    }

    @Override
    public NoraNode liftCache() {
        return new IfNode(cond,then,other, staticRetType);
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        throw new RuntimeException("Should already be Specialised");
    }

    @Override
    public int complexity() {
        return Math.max(then.complexity(), other.complexity());
    }

    @Override
    public String toString() {
        return "if("+cond+", "+then+", "+other+")";
    }

    @Override
    public CondCacheNode cloneUninitialized() {
        return new CondCacheNode(cond.cloneUninitialized(), then.cloneUninitialized(), other.cloneUninitialized(), staticRetType);
    }

    @Override
    public Type getType(SpecFrame frame) {
        var tt = then.getType(frame);
        var ot = other.getType(frame);
        if(tt == ot) return tt;
        assert tt != null && ot != null;
        if(tt.subTypeOf(ot)) return ot;
        if(ot.subTypeOf(tt)) return tt;
        //Fallback to what was provided
        return staticRetType;
    }

    @Override
    public boolean isUnboxed() {
        return then.isUnboxed() || other.isUnboxed();
    }
}
