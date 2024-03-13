package nora.vm.nodes.method;


import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.nodes.utils.ConcreteTypeOfUtilNode;
import nora.vm.nodes.utils.ConcreteTypeOfUtilNodeGen;

public class TypingDispatchNode extends DispatchNode {
    @Child private TypedDispatchNode node;
    @Children private ConcreteTypeOfUtilNode[] typeIndexExtractor;

    public TypingDispatchNode(TypedDispatchNode node, int dispatchArgs) {
        this.typeIndexExtractor = new ConcreteTypeOfUtilNode[dispatchArgs];
        for(int i =0; i < dispatchArgs; i++) typeIndexExtractor[i] = ConcreteTypeOfUtilNodeGen.create();
        this.node = node;
    }

    @ExplodeLoop
    @Override
    public Object executeDispatch(Object target, Object[] arguments) {
        CompilerAsserts.partialEvaluationConstant(typeIndexExtractor.length);
        int[] types = new int[typeIndexExtractor.length];
        CompilerAsserts.partialEvaluationConstant(types.length);
        for(int i = 0; i < typeIndexExtractor.length; i++){
            types[i] = typeIndexExtractor[i].executeIndexTypeOf(arguments[i+1]);
        }
        return node.executeDispatch(target,arguments,types);
    }

    @Override
    public TypingDispatchNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new TypingDispatchNode(node.cloneUninitialized(), typeIndexExtractor.length);
    }
}
