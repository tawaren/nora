package nora.vm.nodes.arr.template;

import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.method.Function;
import nora.vm.method.Method;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.nodes.NoraNode;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.nodes.method.*;
import nora.vm.types.TypeInfo;

public  class HigherOrderNodeUtil {
    public static DispatchNode dispatchNode(Function evaluatedTarget){
        if(evaluatedTarget != null){
            if(evaluatedTarget instanceof MethodLookup ml){
                if(evaluatedTarget instanceof Method){
                    return DirectCallableDispatchNodeGen.create();
                } else {
                    return new TypingDispatchNode(MethodLookupDispatchNodeGen.create(3), ml.dispatchedArgs());
                }
            } else {
                return DirectCallableDispatchNodeGen.create();
            }
        } else {
            return FlexibleDispatchNodeGen.create();
        }
    }
    public static Function evaluatedFunction(ConstNode nFunction, TypeInfo[] args) throws UnexpectedResultException {
        var evaluatedTarget = nFunction.executeFunction(null);
        if(evaluatedTarget instanceof MethodLookup ml){
            var res = ml.staticLookup(args);
            if(res != null) evaluatedTarget = res;
        }
        return evaluatedTarget;
    }
}
