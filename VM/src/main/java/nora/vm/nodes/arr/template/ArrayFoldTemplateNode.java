package nora.vm.nodes.arr.template;

import nora.vm.method.Function;
import nora.vm.nodes.IntIndexNodeGen;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.arr.ArrayFoldLNode;
import nora.vm.nodes.arr.ArrayFoldLNodeGen;
import nora.vm.nodes.arr.ArrayFoldRNodeGen;
import nora.vm.nodes.arr.ArrayGenerateNodeGen;
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
public class ArrayFoldTemplateNode extends TemplateNode {
    private final NoraNode initial;
    private final NoraNode array;
    private final  NoraNode function;
    private final boolean isLeft;

    public ArrayFoldTemplateNode(NoraNode initial, NoraNode array, NoraNode function, boolean isLeft) {
        this.initial = initial;
        this.array = array;
        this.function = function;
        this.isLeft = isLeft;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        var util = NoraVmContext.getTypeUtil(null);
        var nInitial = initial.specialise(frame);
        var nArray = array.specialise(frame);
        var nFunction = function.specialise(frame);
        frame.markNonTrivial();
        Function evaluatedTarget = null;
        if(nFunction instanceof ConstNode cn){
            evaluatedTarget = HigherOrderNodeUtil.evaluatedFunction(cn, new TypeInfo[]{util.IntTypeInfo});
            if(evaluatedTarget != null) nFunction = ConstNode.create(evaluatedTarget);
        }
        DispatchNode dispatch = HigherOrderNodeUtil.dispatchNode(evaluatedTarget);

        if(nInitial instanceof CachedNode ci && nArray instanceof CachedNode ca &&  nFunction instanceof CachedNode cf){
           if(isLeft){
               return new CacheNode(ArrayFoldLNodeGen.create(dispatch, ci.liftCache(), ca.liftCache(), cf.liftCache()));
           } else {
               return new CacheNode(ArrayFoldRNodeGen.create(dispatch, ca.liftCache(), ci.liftCache(), cf.liftCache()));
           }
        } else {
            if(isLeft){
                return ArrayFoldLNodeGen.create(dispatch, nInitial, nArray, nFunction);
            } else {
                return ArrayFoldRNodeGen.create(dispatch, nArray, nInitial, nFunction);
            }
        }
    }
}
