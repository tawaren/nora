package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.runtime.data.NoraData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;

public class CreateNode extends ExecutionNode {
    private final int dispatcherIndex;
    private final DataSchemaHandler handler;
    @Children private final NoraPropertySetNode[] argumentSetters;

    public CreateNode(int dispatcherIndex, DataSchemaHandler handler, NoraPropertySetNode[] argumentSetters) {
        this.handler = handler;
        this.argumentSetters = argumentSetters;
        this.dispatcherIndex = dispatcherIndex;
    }

    @ExplodeLoop
    @Override
    public NoraData executeNoraData(VirtualFrame frame) {
        var data = handler.create(dispatcherIndex);
        CompilerAsserts.partialEvaluationConstant(argumentSetters.length);
        for(int i = 0; i < argumentSetters.length; i++){
            argumentSetters[i].executeSet(frame, data);
        }
        return data;
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        return executeNoraData(virtualFrame);
    }

    @Override
    public String toString() {
        var args = Arrays.stream(argumentSetters).map(Object::toString).toList();
        return handler.type+"("+String.join(", ",args)+"}";
    }

    @Override
    public CreateNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        var nArgs = new NoraPropertySetNode[argumentSetters.length];
        for(int i = 0;i < nArgs.length; i++) nArgs[i] = argumentSetters[i].cloneUninitialized();
        return new CreateNode(dispatcherIndex, handler, nArgs);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return handler.type;
    }

    @Override
    public int complexity() {
        var count = 1;
        for (NoraPropertySetNode n:argumentSetters){
            count+= (1+n.getValueNode().complexity());
        }
        return count;
    }
}
