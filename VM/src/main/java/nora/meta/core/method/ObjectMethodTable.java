package nora.meta.core.method;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import nora.meta.core.PrivateObjectLayout;
import nora.vm.method.Method;
import nora.vm.method.lookup.DispatchTable;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class ObjectMethodTable extends DispatchTable {
    @CompilationFinal(dimensions = 1) private Method[] table;
    private final ObjectMethod source;
    public ObjectMethodTable(ObjectMethod source) {
        this.table = new Method[source.rootLayout.getPrivateImplementationCount()];
        this.source = source;
    }

    public static DispatchTable create(ObjectMethod source) {
        return new ObjectMethodTable(source);
    }

    @TruffleBoundary
    private Method loadMethod(PrivateObjectLayout handler){
        var sig = handler.fullObjectSignature();
        var generics = source.generics;
        var nSig = new Type[sig.length+generics.length];
        System.arraycopy(sig, 0, nSig, 0, sig.length);
        System.arraycopy(generics,0,nSig, sig.length, generics.length);
        var method =  NoraVmContext.getMethodFactory(null).create(source.methodId, nSig);
        table[handler.getDispatchId()] = method;
        return method;
    }

    @TruffleBoundary
    private void adaptTable() {
        var nSize = source.rootLayout.getPrivateImplementationCount();
        assert nSize > table.length;
        var nTable = new Method[nSize];
        System.arraycopy(table,0,nTable,0, table.length);
        table = nTable;
    }

    @Override
    public Method runtimeLookup(Object[] args) {
        assert args.length > 0;
        assert args[0] instanceof RuntimeData;
        assert ((RuntimeData)args[0]).handler instanceof PrivateObjectLayout;
        var handler = (PrivateObjectLayout)((RuntimeData)args[0]).handler;
        int id = handler.getDispatchId();
        if(id < table.length){
            var method = table[id];
            if(method != null) return method;
        } else {
            transferToInterpreterAndInvalidate();
            assert id < source.rootLayout.getPrivateImplementationCount();
            adaptTable();
        }
        transferToInterpreterAndInvalidate();
        return loadMethod(handler);
    }
}
