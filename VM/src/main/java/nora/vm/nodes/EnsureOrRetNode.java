package nora.vm.nodes;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ControlFlowException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.cache.CondCacheNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class EnsureOrRetNode extends ForwardNode{

    public static class EarlyReturnException extends ControlFlowException {
        private final Object value;

        public EarlyReturnException(Object value) {
            this.value = value;
        }

        public Object getValue() {
            return value;
        }
    }

    @Child private NoraNode cond;
    @Child private NoraNode retValue;
    @Child private NoraNode then;
    private final CountingConditionProfile condition = CountingConditionProfile.create();

    public EnsureOrRetNode(NoraNode cond, NoraNode retValue, NoraNode then) {
        this.cond = cond;
        this.retValue = retValue;
        this.then = then;
    }

    private boolean executeCond(VirtualFrame frame){
        try {
            return cond.executeBoolean(frame);
        } catch (UnexpectedResultException e) {
            CompilerDirectives.transferToInterpreterAndInvalidate();
            //We can not recover from this
            throw new IllegalArgumentException("If's are only defined on booleans");
        }
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        if(condition.profile(executeCond(virtualFrame))){
            return then.execute(virtualFrame);
        } else {
            throw new EarlyReturnException(retValue.execute(virtualFrame));
        }
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeBoolean(virtualFrame);
        } else {
            throw new EarlyReturnException(retValue.execute(virtualFrame));
        }
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeByte(virtualFrame);
        } else {
            throw new EarlyReturnException(retValue.execute(virtualFrame));
        }
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeInt(virtualFrame);
        } else {
            throw new EarlyReturnException(retValue.execute(virtualFrame));
        }
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        if(condition.profile(executeCond(virtualFrame))){
            return then.executeLong(virtualFrame);
        } else {
            throw new EarlyReturnException(retValue.execute(virtualFrame));
        }
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var nCond = cond.specialise(frame);
        if (nCond instanceof ConstNode cn){
            var res = cn.executeBoolean(null);
            if(res){
                return then.specialise(frame);
            } else {
                var nRet = retValue.specialise(null);
                if(nRet instanceof ConstNode rn) {
                    throw new EarlyReturnException(rn.execute(null));
                } else {
                    return new TypedRetNode(nRet, then.getType(frame));
                }
            }
        }
        var nThen = then.safeSpecialise(frame);
        var nRet = retValue.safeSpecialise(frame);
        if(nCond instanceof CachedNode nc){
            var retT = nThen.getType(frame);
            return new CondCacheNode(nc.liftCache(),nThen,new TypedRetNode(nRet, retT), retT);
        }
        return new EnsureOrRetNode(nCond, nThen, nRet);
    }

    @Override
    public String toString() {
        return "ensure("+cond+" then "+then+" else return "+retValue+")";
    }

    @Override
    public EnsureOrRetNode cloneUninitialized() {
        return new EnsureOrRetNode(cond.cloneUninitialized(), then.cloneUninitialized(), retValue.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        return then.getType(frame);
    }

    @Override
    public boolean isUnboxed() {
        return then.isUnboxed();
    }

    @Override
    public int complexity() {
        return 1+cond.complexity()+then.complexity();
    }
}
