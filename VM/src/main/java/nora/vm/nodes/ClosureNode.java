package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import nora.vm.method.EntryPoint;
import nora.vm.method.Function;
import nora.vm.nodes.property.setter.NoraPropertySetNode;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.schemas.ClosureSchemaHandler;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.util.Arrays;

public class ClosureNode extends ExecutionNode {
    private final EntryPoint code;
    private final Type funType;
    //Used as quick checkable identifier for different closures
    private final IdentityObject closureLabel;
    private final ClosureSchemaHandler handler;
    @Children private final NoraPropertySetNode[] captures;

    public ClosureNode(NoraPropertySetNode[] captures, EntryPoint code, Type funType, IdentityObject closureLabel, ClosureSchemaHandler handler) {
        this.captures = captures;
        this.code = code;
        this.funType = funType;
        this.closureLabel = closureLabel;
        this.handler = handler;
    }

    @ExplodeLoop
    private ClosureData createClosure(VirtualFrame frame){
        var data = handler.create(code, funType, closureLabel);
        CompilerAsserts.partialEvaluationConstant(captures.length);
        for(int i = 0; i < captures.length; i++){
            captures[i].executeSet(frame, data);
        }
        return data;
    }

    @Override
    public Function executeFunction(VirtualFrame frame) {
        return createClosure(frame);
    }

    @Override
    public NoraData executeNoraData(VirtualFrame frame) {
        return createClosure(frame);
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return createClosure(frame);
    }

    @Override
    public String toString() {
        var args = Arrays.stream(captures).map(Object::toString).toList();
        return "closure("+String.join(", ",args)+"){"+code+"}";
    }

    @Override
    public ClosureNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        var nCapt = new NoraPropertySetNode[captures.length];
        for(int i = 0;i < nCapt.length; i++) nCapt[i] = captures[i].cloneUninitialized();
        //Todo: not sure if code needs cloning as is behind a call target anyway
        return new ClosureNode(nCapt, code /*.cloneUninitialized()*/, funType, closureLabel, handler);
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return funType;
    }

    @Override
    public int complexity() {
        var count = 1;
        for (NoraPropertySetNode n:captures){
            count+= (1+n.getValueNode().complexity());
        }
        return count;
    }
}
