package nora.vm.nodes.method.opt.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.consts.TypeNode;
import nora.vm.nodes.method.opt.DelayedArgSetterNode;
import nora.vm.nodes.method.opt.TailLoopCall;
import nora.vm.nodes.method.opt.TailLoopDataCall;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

public class GenericTailLoopDataCall extends TemplateNode {
    private final String id;
    private final NoraNode[] genTypeExprs;
    //1 of this will be the TailLoopCall
    private final NoraNode[] arguments;

    private int ctxSlot;

    public GenericTailLoopDataCall(String id, NoraNode[] genTypeExprs, NoraNode[] arguments, int ctxSlot) {
        this.id = id;
        this.genTypeExprs = genTypeExprs;
        this.arguments = arguments;
        this.ctxSlot = ctxSlot;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        Type[] generics = new Type[genTypeExprs.length];
        for (int i = 0; i < genTypeExprs.length; i++) {
            NoraNode gen = genTypeExprs[i];
            NoraNode newType = gen.specialise(frame);
            if (newType instanceof TypeNode tn){
                generics[i] = tn.getType();
            } else {
                throw new IllegalStateException("Dynamic types are not supported");
            }
        }
        NoraNode[] newArgs = new NoraNode[arguments.length];
        DelayedArgSetterNode argSetter = null;
        for (int i = 0; i < arguments.length; i++) {
            newArgs[i] = arguments[i].specialise(frame);
            if(newArgs[i] instanceof TailLoopCall tlc){
                assert argSetter == null;
                argSetter = tlc.getArgSetters();
                newArgs[i] = null;
            }
        }
        assert argSetter != null;
        var handler =  getContext().getSchemaManager().getDataHandlerFor(id, generics);
        var coordinator = NoraVmContext.getDispatchCoordinator(null);
        int dispatcherIndex = coordinator.assignIndex(handler.type, getContext().getLoader());
        return TailLoopDataCall.constructTailLoopDataCall(ctxSlot, argSetter, dispatcherIndex, handler, newArgs);

    }
}
