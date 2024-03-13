package nora.vm.nodes.arr.template;

import nora.vm.method.Function;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.arr.ArrayFlatMapNodeGen;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.method.DispatchNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

//Todo: needs a template that produces call Node
public class ArrayFlatMapTemplateNode extends TemplateNode {
    private final NoraNode array;
    private final  NoraNode function;

    public ArrayFlatMapTemplateNode(NoraNode array, NoraNode function) {
        this.array = array;
        this.function = function;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var util = NoraVmContext.getTypeUtil(null);
        var nArray = array.specialise(frame);
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
        if(nArray instanceof CachedNode ca &&  nFunction instanceof CachedNode cf){
            return new CacheNode(ArrayFlatMapNodeGen.create(dispatch, resultType, ca.liftCache(), cf.liftCache()));
        } else {
            return new CacheNode(ArrayFlatMapNodeGen.create(dispatch, resultType, nArray, nFunction));
        }
    }
}
