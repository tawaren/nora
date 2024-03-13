package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.frame.VirtualFrame;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class TailLoopCall extends NoraNode {

    @Child private DelayedArgSetterNode argSetters;

    public TailLoopCall(DelayedArgSetterNode argSetters) {
        this.argSetters = argSetters;
    }

    public DelayedArgSetterNode getArgSetters() {
        return argSetters;
    }

    public Object execute(VirtualFrame frame){
        if(argSetters != null) argSetters.execute(frame);
        //null indicates next loop
        return null;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        return new TailLoopCall(argSetters.specialise(frame));
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new TailLoopCall(argSetters.cloneUninitialized());
    }

    @Override
    public int complexity() {
        var count = 1+argSetters.complexity();
        return count;
    }

    @Override
    public Type getType(SpecFrame frame) {
        return frame.getRetType();
    }
}
