package nora.vm.nodes.method;

import com.oracle.truffle.api.CallTarget;
import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.dsl.GenerateInline;
import com.oracle.truffle.api.dsl.ReportPolymorphism;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.api.nodes.IndirectCallNode;
import nora.vm.method.Callable;
import nora.vm.method.MultiMethod;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.runtime.data.ClosureData;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreter;

@ReportPolymorphism
@GenerateInline(false)
public abstract class FlexibleDispatchNode extends DispatchNode {

    @Specialization(guards = "cachedTarget.getTarget() == target.getTarget()", limit = "3")
    public Object dynamicClosureCallable(ClosureData target, Object[] args,
                                        @Cached("target") ClosureData cachedTarget,
                                        @Cached("createDispatch(cachedTarget)") DispatchNode dispatcher
    ){
        return dispatcher.executeDispatch(cachedTarget, args);
    }

    //Note: we only do 2 as multi methods can generate another layer with up to 3
    //  The virtual dispatch is fine, as it is on a cached, which is compiletime static (meaning its inlinable static dispatch)
    @Specialization(guards = "cachedTarget == target", limit = "2")
    public Object dynamicMethodCallable(MethodLookup target, Object[] args,
                                        @Cached("target") MethodLookup cachedTarget,
                                        @Cached("createDispatch(cachedTarget)") DispatchNode dispatcher
    ){
        return dispatcher.executeDispatch(cachedTarget, args);
    }

    //Note: we only do 2 as multi methods can generate another layer with up to 3
    //  The virtual dispatch is fine, as it is on a cached, which is compiletime static (meaning its inlinable static dispatch)
    @Specialization(replaces = {"dynamicMethodCallable", "dynamicClosureCallable"}, guards = "sameTarget(cachedTarget, target)", limit = "2")
    public Object dynamicMixedCallable(Object target, Object[] args,
                                        @Cached("target") Object cachedTarget,
                                        @Cached("createDispatch(cachedTarget)") DispatchNode dispatcher
    ){
        return dispatcher.executeDispatch(cachedTarget, args);
    }

    @Specialization
    public Object dynamicUncached(Object target, Object[] args,
                                  @Cached(neverDefault = true, value = "initIndirectCall()") IndirectCallNode indirectCallNode
    ){
        CallTarget trg;
        if(target instanceof Callable cal){
            trg = cal.getTarget();
        } else if(target instanceof MethodLookup md){
            //will internally be optimized if called enough (at least if MultiMethod)
            trg = md.slowRuntimeLookup(args).getTarget();
        } else {
            transferToInterpreter();
            throw new IllegalArgumentException("A non callable was called");
        }
        return callIndirect(indirectCallNode, trg, args);
    }

    protected final boolean sameTarget(Object cachedTarget, Object target){
        if(cachedTarget == target) return true;
        if(cachedTarget instanceof ClosureData cd1 && target instanceof ClosureData cd2){
            return cd1.getTarget() == cd2.getTarget();
        } else {
            return false;
        }
    }

    protected DispatchNode createDispatch(Object method){
        if(method instanceof Callable){
            return DirectCallableDispatchNodeGen.create();
        } else if(method instanceof MultiMethod md){
            return new TypingDispatchNode(MethodLookupDispatchNodeGen.create(2),md.dispatchedArgs());
        } else {
            transferToInterpreter();
            throw new IllegalArgumentException("A non callable was called");
        }
    }

    @Override
    public String toString() {
        return "DynamicDispatch";
    }

    @Override
    public FlexibleDispatchNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return FlexibleDispatchNodeGen.create();
    }
}
