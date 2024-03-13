package nora.vm.nodes.method.opt;

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.nodes.CreateNode;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.runtime.data.NoraData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.SchemaManager;

public class TailLoopDataCall extends ExecutionNode {

    @Child private DelayedArgSetterNode argSetters;
    @Child private CreateNode creator;

    @Child private CtrContextAdaptNode adapter = CtrContextAdaptNodeGen.create() ;

    private final StaticProperty prop;
    private final int ctxSlot;

    private TailLoopDataCall(DelayedArgSetterNode argSetters, CreateNode creator, int ctxSlot, StaticProperty prop) {
        this.argSetters = argSetters;
        this.creator = creator;
        this.prop = prop;
        this.ctxSlot = ctxSlot;
    }

    public static TailLoopDataCall constructTailLoopDataCall(int ctxSlot, DelayedArgSetterNode argSetters, int dispatcherIndex, DataSchemaHandler handler, NoraNode[] args) {
        var ctrFields = new NoraPropertySetNode[args.length-1];
        var props = handler.getAllProperties();
        StaticProperty target = null;
        int offset = 0;
        for(int i = 0; i < args.length; i++){
            var arg = args[i];
            if(arg == null){
                assert props[i] instanceof SchemaManager.ObjectProperty;
                target = ((SchemaManager.ObjectProperty)props[i]).getProperty();
                offset+=1;
            } else {
                ctrFields[i-offset] = props[i].getSetter(arg);
            }
        }
        assert offset == 1;
        return new TailLoopDataCall(argSetters,new CreateNode(dispatcherIndex, handler, ctrFields), ctxSlot, target);
    }

    public Object execute(VirtualFrame frame){
        var data = creator.executeNoraData(frame);
        if(argSetters != null) argSetters.execute(frame);
        var cur = frame.getObjectStatic(ctxSlot);
        if(cur != null) {
            adapter.execute(
                    (NoraData) cur,
                    (StaticProperty) frame.getObjectStatic(ctxSlot+1),
                    data);
        }
        frame.setObjectStatic(ctxSlot, data);
        frame.setObjectStatic(ctxSlot+1, prop);
        //CtrContext indicates next loop
        return null;
    }

    @Override
    public NoraNode cloneUninitialized() {
        return new TailLoopDataCall(argSetters.cloneUninitialized(), creator.cloneUninitialized(), ctxSlot, prop);
    }

    @Override
    public int complexity() {
        return 1+argSetters.complexity()+creator.complexity();
    }

    @Override
    public Type getType(SpecFrame frame) {
        return frame.getRetType();
    }
}
