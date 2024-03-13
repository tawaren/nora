package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.data.NoraData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class TailLoopDataHeader extends NoraNode {
    @Child private NoraNode body;
    @Children private final ArgSetterNode[] argSetters;
    private final int argStartSlot;
    private final int ctxSlot;
    public TailLoopDataHeader(int argStartSlot, ArgSetterNode[] argSetters, NoraNode body) {
        this.argSetters = argSetters;
        this.body = body;
        this.argStartSlot = argStartSlot;
        this.ctxSlot = argStartSlot+argSetters.length;
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
        //Loops until the first Context tail call or final result
        //      is needed because we need to capture it so we can return it at the end
        Object res;
        do{
            res = body.execute(frame);
            if(res == null) res = frame.getObjectStatic(ctxSlot);
            else return res;
        } while (res == null);
        //Loops until result
        Object next;
        do {
            next = body.execute(frame);
        } while (next == null);
        //Integrates final result (for example Nil in a List) into the Context
        ((StaticProperty) frame.getObjectStatic(ctxSlot+1)).setObject(
                frame.getObjectStatic(ctxSlot),
                next
        );
        return res;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        for(int i = 0; i < argSetters.length; i++){
            assert frame.isKnownArgumentTyp(i);
            assert !frame.isLocalKnownTyp(i);
            frame.setType(argStartSlot+i,frame.getArgumentTyp(i));
        }
        frame.setType(argStartSlot+argSetters.length, null);
        frame.setType(argStartSlot+argSetters.length+1, null);
        frame.markNonTrivial();
        var nBody = body.specialise(frame);
        if(nBody instanceof ConstNode cn){
            var res = cn.execute(null);
            if(res == null) return new TailLoopDataHeader(argStartSlot, argSetters, nBody);
            return cn;
        }
        if(nBody instanceof CachedNode cn){
            return new CacheNode(new TailLoopDataHeader(argStartSlot, argSetters, cn.liftCache()));
        }
        return new TailLoopDataHeader(argStartSlot, argSetters, nBody);
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new TailLoopDataHeader(argStartSlot, argSetters, body.cloneUninitialized());
    }

    @Override
    public Type getType(SpecFrame frame) {
        return body.getType(frame);
    }
}
