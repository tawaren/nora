package nora.vm.nodes.method.template;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.method.Function;
import nora.vm.method.Method;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.nodes.cache.CacheNode;
import nora.vm.nodes.cache.CachedNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.template.TemplateNode;
import nora.vm.nodes.type.SwitchNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.types.TypeUtil;
import nora.vm.runtime.data.ClosureData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.nodes.method.NoraCallNode;
import nora.vm.types.TypeInfo;

import java.util.Arrays;

public class GenericDispatchNode extends TemplateNode {
    //For now a null call target indicates a tail call
    private final NoraNode callTarget;
    private final NoraNode[] arguments;

    public GenericDispatchNode(NoraNode callTarget, NoraNode[] arguments) {
        this.callTarget = callTarget;
        this.arguments = arguments;
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        assert callTarget != null;
        var newArgs = new NoraNode[arguments.length];
        var argTypes = new TypeInfo[arguments.length];
        var target = callTarget.specialise(frame);
        Type.TypeParameter[] sig;
        Function evaluatedTarget = null;
        var allArgsCached = true;
        if(target instanceof ConstNode){
            evaluatedTarget = target.executeFunction(null);
            sig = evaluatedTarget.getType().applies;
        } else {
            if(target instanceof CachedNode) allArgsCached = false;
            sig = target.getType(frame).applies;
        }
        var concreteArgTypes = evaluatedTarget != null;
        for (int i = 0; i < arguments.length; i++) {
            var res = arguments[i].specialise(frame);
            if(allArgsCached && !(res instanceof CachedNode)) allArgsCached = false;
            if(concreteArgTypes){
                Type typ = res.getType(frame);
                if(typ != null && typ.isConcrete()) {
                    argTypes[i] = typ.info;
                } else if(sig[i].type().isConcrete()) {
                    argTypes[i] = sig[i].type().info;
                } else {
                    concreteArgTypes = false;
                }
            }
            newArgs[i] = res;
            if(argTypes[i] != null){
                newArgs[i] = SwitchNode.safeCreate(argTypes[i], res);
            }
        }

        //We need not to cache args if we cache result
        if(allArgsCached) {
            for (int i = 0; i < newArgs.length; i++) {
                newArgs[i] = ((CachedNode)newArgs[i]).liftCache();
            }
        }

        NoraNode callNode;
        if(evaluatedTarget != null){
            if(evaluatedTarget instanceof MethodLookup ml){
                if(concreteArgTypes) {
                    var res = ml.staticLookup(argTypes);
                    if(res != null) evaluatedTarget = res;
                }
                if(evaluatedTarget instanceof Method){
                    var specialTarget = NoraVmContext.getSpecialMethodRegistry(null).resolveSpecialCall((Method) evaluatedTarget, newArgs);
                    if(specialTarget == null) {
                        if(!allArgsCached)frame.markNonTrivial();
                        callNode = NoraCallNode.createDirectMethodCall((Method) evaluatedTarget, newArgs);
                    } else {
                        callNode = specialTarget.specialise(frame);
                    }
                } else {
                    if(!allArgsCached)frame.markNonTrivial();
                    callNode = NoraCallNode.createLookupCall((MethodLookup)evaluatedTarget, newArgs);
                }
            } else {
                if(!allArgsCached)frame.markNonTrivial();
                callNode = NoraCallNode.createDirectClosureCall((ClosureData) evaluatedTarget, newArgs);
            }
        } else {
            //Todo: This one can actually lead to 2 calls to getType() on target
            //      In theory this can have non Constant runtime
            if(!allArgsCached)frame.markNonTrivial();
            callNode = NoraCallNode.createDynamicCall(target,newArgs);
        }
        if(allArgsCached){
            callNode = new CacheNode(callNode);
        }
        return callNode;
    }

    @Override
    public String toString() {
        var args = Arrays.stream(arguments).map(Object::toString).toList();
        return callTarget+"(" + String.join(",", args) +")";
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        TypeUtil util = NoraVmContext.getTypeUtil(null);
        var trg = callTarget.getType(frame);
        assert trg != null;
        assert trg.info == util.FunctionTypeInfo;
        var applies = trg.applies;
        return applies[applies.length-1].type();
    }
}
