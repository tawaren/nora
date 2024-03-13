package nora.vm.nodes.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.ValNodeGen;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.TypeUtil;


public class ValNodeUntyped extends TemplateNode {
    private final int slot;

    public ValNodeUntyped(int slot) {
        this.slot = slot;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        if(frame.isLocalKnownVal(slot)) {
            var constVal = frame.getValue(slot);
            return ConstNode.create(constVal);
        }
        assert frame.isLocalKnownTyp(slot);
        if(frame.isValCached(slot)){
            return new CacheNode(ValNodeGen.create(slot, util.getKind(frame.getTyp(slot).info)));
        } else {
            return ValNodeGen.create(slot, util.getKind(frame.getTyp(slot).info));
        }
    }

    @Override
    public String toString() {
        return "Val("+slot+")";
    }

}
