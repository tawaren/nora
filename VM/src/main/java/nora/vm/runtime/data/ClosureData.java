package nora.vm.runtime.data;


import com.oracle.truffle.api.RootCallTarget;
import com.oracle.truffle.api.dsl.Cached;
import com.oracle.truffle.api.interop.ArityException;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.library.ExportLibrary;
import com.oracle.truffle.api.library.ExportMessage;
import com.oracle.truffle.api.nodes.DirectCallNode;
import nora.vm.method.Callable;
import nora.vm.method.EntryPoint;
import nora.vm.types.Type;
import nora.vm.types.TypeInfo;
import nora.vm.types.schemas.ClosureSchemaHandler;

@ExportLibrary(InteropLibrary.class)
public class ClosureData extends NoraData implements Callable {
    public final RootCallTarget callTarget;
    public final Type funType;
    public final Object identity;

    public ClosureData(EntryPoint target, Type funType, Object identity, ClosureSchemaHandler handler) {
        super(handler);
        this.callTarget = target.getCallTarget();
        this.funType = funType;
        this.identity = identity;
    }

    @Override
    public RootCallTarget getTarget() {
        return callTarget;
    }

    @Override
    public String toString() {
        return "["+callTarget+"]";
    }

    @Override
    public Type getType() {
        return funType;
    }

    @Override
    public TypeInfo getTypeInfo() {
        return funType.info;
    }

    @ExportMessage
    public boolean isExecutable(){
        return true;
    }

    @ExportMessage
    public Object execute(Object[] arguments,
                          @Cached(value = "this.createCallNode()", uncached = "this.createCallNode()", neverDefault = true) DirectCallNode callNode
    ) throws ArityException {
        var argsSize = funType.applies.length -1;
        if(arguments.length != argsSize) throw ArityException.create(argsSize,argsSize, arguments.length);
        return callNode.call(arguments);
    }

    public DirectCallNode createCallNode(){
        return DirectCallNode.create(callTarget);
    }

}
