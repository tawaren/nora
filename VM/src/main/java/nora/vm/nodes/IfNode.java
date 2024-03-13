package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
//import com.oracle.truffle.api.profiles.CountingConditionProfile;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class IfNode extends ForwardNode{
    @Child private NoraNode cond;
    @Child private NoraNode then;
    @Child private NoraNode other;
    private final Type staticRetType;

    private final CountingConditionProfile condition = CountingConditionProfile.create();

    public IfNode(NoraNode cond, NoraNode then, NoraNode other, Type staticRetType) {
        this.cond = cond;
        this.then = then;
        this.other = other;
        this.staticRetType = staticRetType;
    }

    private boolean executeCond(VirtualFrame frame){
        try {
            return cond.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreter();
            //We can not recover from this
            throw new IllegalArgumentException("If's are only defined on booleans");
        }
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        if(condition.profile(executeCond(virtualFrame))){
            return then.execute(virtualFrame);
        } else {
            return other.execute(virtualFrame);
        }
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeBoolean(virtualFrame);
        } else {
            return other.executeBoolean(virtualFrame);
        }
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeByte(virtualFrame);
        } else {
            return other.executeByte(virtualFrame);
        }
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeInt(virtualFrame);
        } else {
            return other.executeInt(virtualFrame);
        }
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeLong(virtualFrame);
        } else {
            return other.executeLong(virtualFrame);
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        throw new RuntimeException("Should already be Specialised");
    }

    @Override
    public String toString() {
        return "if("+cond+", "+then+", "+other+")";
    }

    @Override
    public IfNode cloneUninitialized() {
        return new IfNode(cond.cloneUninitialized(), then.cloneUninitialized(), other.cloneUninitialized(), staticRetType);
    }

    @Override
    public Type getType(SpecFrame frame) {
        var tt = then.getType(frame);
        var ot = other.getType(frame);
        if(tt == ot) return tt;
        //these can happen if one branch returns or throws a spec error
        if(ot == null) return tt;
        if(tt == null) return ot;
        if(tt.subTypeOf(ot)) return ot;
        if(ot.subTypeOf(tt)) return tt;
        //Fallback to what was provided
        return staticRetType;
    }

    @Override
    public boolean isUnboxed() {
        return then.isUnboxed() || other.isUnboxed();
    }

    @Override
    public int complexity() {
        return 1+cond.complexity()+Math.max(then.complexity(),other.complexity());
    }
}
