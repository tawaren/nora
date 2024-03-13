package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.profiles.CountingConditionProfile;
import nora.vm.nodes.IfNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.cache.CondCacheNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class UntypedIfNode extends TemplateNode {
    private final NoraNode cond;
    private final NoraNode then;
    private final NoraNode other;
    private final NoraNode staticRetType;

    public UntypedIfNode(NoraNode cond, NoraNode then, NoraNode other, NoraNode staticRetType) {
        this.cond = cond;
        this.then = then;
        this.other = other;
        this.staticRetType = staticRetType;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        var newCond = cond.specialise(frame);
        var newTyp = staticRetType.specialise(frame);
        Type typ;
        if (newTyp instanceof TypeNode tn){
            typ = tn.getType();
        } else {
            throw new IllegalStateException("Dynamic types are not supported");
        }
        if(newCond instanceof ConstNode){
            if(newCond.executeBoolean(null)){
                return then.specialise(frame);
            } else {
                return other.specialise(frame);
            }
        } else {
            var newThen = then.safeSpecialise(frame);
            var newOther = other.safeSpecialise(frame);
            if(newCond instanceof CachedNode cn1){
                return new CacheNode(new CondCacheNode(cn1.liftCache(),newThen,newOther,typ));
            } else {
                return new IfNode(newCond,newThen,newOther,typ);
            }
        }
    }

    @Override
    public String toString() {
        return "if("+cond+", "+then+", "+other+")";
    }

    @Override
    public UntypedIfNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new UntypedIfNode(cond.cloneUninitialized(), then.cloneUninitialized(), other.cloneUninitialized(), staticRetType);
    }

}
