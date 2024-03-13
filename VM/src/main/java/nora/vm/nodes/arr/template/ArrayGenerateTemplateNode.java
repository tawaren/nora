package nora.vm.nodes.arr.template;

import nora.vm.method.Function;
import nora.vm.nodes.IntIndexNodeGen;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.arr.ArrayGenerateNodeGen;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.method.*;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

//Todo: needs a template that produces call Node
public class ArrayGenerateTemplateNode extends TemplateNode {
    private final NoraNode length;
    private final  NoraNode function;

    public ArrayGenerateTemplateNode(NoraNode length, NoraNode function) {
        this.length = length;
        this.function = function;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var util = NoraVmContext.getTypeUtil(null);
        var nLength = IntIndexNodeGen.create(length).specialise(frame);
        var nFunction = function.specialise(frame);
        var funTypeParams = nFunction.getType(frame).applies;
        var resultType = new Type(util.ArrayTypeInfo, new Type.TypeParameter[]{funTypeParams[funTypeParams.length-1]});
        frame.markNonTrivial();
        Function evaluatedTarget = null;
        if(nFunction instanceof ConstNode cn){
            evaluatedTarget = HigherOrderNodeUtil.evaluatedFunction(cn, new TypeInfo[]{util.IntTypeInfo});
            if(evaluatedTarget != null) nFunction = ConstNode.create(evaluatedTarget);
        }
        DispatchNode dispatch = HigherOrderNodeUtil.dispatchNode(evaluatedTarget);
        if(nLength instanceof CachedNode cl && nFunction instanceof CachedNode cf){
            return new CacheNode(ArrayGenerateNodeGen.create(dispatch,resultType,cl.liftCache(),cf.liftCache()));
        } else {
            return ArrayGenerateNodeGen.create(dispatch,resultType,nLength,nFunction);
        }
    }
}
