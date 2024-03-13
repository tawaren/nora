package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class TailLoopHeader extends NoraNode {
    @Child private NoraNode body;
    @Children private final ArgSetterNode[] argSetters;
    private final int argStartSlot;
    public TailLoopHeader(int argStartSlot, ArgSetterNode[] argSetters, NoraNode body) {
        this.argSetters = argSetters;
        this.body = body;
        this.argStartSlot = argStartSlot;
    }

    @ExplodeLoop
    public void init(VirtualFrame frame){
        for(int i = 0; i < argSetters.length; i++){
            argSetters[i].execute(frame);
        }
    }

    @Override
    public Object execute(VirtualFrame frame) {
        init(frame);
        Object res;
        do {
            res = body.execute(frame);
        } while (res == null);
        return res;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        for(int i = 0; i < argSetters.length; i++){
            assert frame.isKnownArgumentTyp(i);
            assert !frame.isLocalKnownTyp(i);
            frame.setType(argStartSlot+i,frame.getArgumentTyp(i));
        }
        frame.markNonTrivial();
        var nBody = body.specialise(frame);
        if(nBody instanceof ConstNode cn){
            var res = cn.execute(null);
            if(res == null) return new TailLoopHeader(argStartSlot, argSetters, nBody);
            return cn;
        }
        if(nBody instanceof CachedNode cn){
            return new CacheNode(new TailLoopHeader(argStartSlot, argSetters, cn.liftCache()));
        }
        return new TailLoopHeader(argStartSlot, argSetters, nBody);
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new TailLoopHeader(argStartSlot, argSetters, body.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        return body.getType(frame);
    }
}
