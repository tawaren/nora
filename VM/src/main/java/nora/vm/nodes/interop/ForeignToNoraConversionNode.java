package nora.vm.nodes.interop;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.interop.InteropLibrary;
import com.oracle.truffle.api.interop.UnsupportedMessageException;
import com.oracle.truffle.api.library.CachedLibrary;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.strings.TruffleString;

import nora.vm.method.Callable;
import nora.vm.method.EntryPoint;
import nora.vm.method.lookup.MethodLookup;
import nora.vm.nodes.ArgNode;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.ClosureDataFactory;
import nora.vm.types.TypeUtil;
import nora.vm.runtime.data.ClosureData;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.schemas.ClosureSchemaHandler;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

@ImportStatic(TypeUtil.TypeKind.class)
@NodeChild(value = "argNode", type = NoraNode.class)
public abstract class ForeignToNoraConversionNode extends ExecutionNode {
    abstract NoraNode getArgNode();
    protected final Type typ;
    protected final TypeUtil.TypeKind kind;

    public ForeignToNoraConversionNode(Type typ) {
        this.typ = typ;
        this.kind = NoraVmContext.getTypeUtil(null).getKind(typ.info);
    }

    @Specialization(guards = {"kind == BOOL", "argument.isBoolean(b)"}, limit ="3")
    public boolean convertBool(Object b, @CachedLibrary("b") InteropLibrary argument) {
        try {
            return argument.asBoolean(b);
        } catch (UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error");
        }
    }

    @Specialization(guards = {"kind == BYTE", "argument.fitsInByte(b)"}, limit ="3")
    public byte convertByte(Object b, @CachedLibrary("b") InteropLibrary argument) {
        try {
            return argument.asByte(b);
        } catch (UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error");
        }
    }

    @Specialization(guards = {"kind == INT", "argument.fitsInInt(i)"}, limit ="3")
    public int convertInt(Object i, @CachedLibrary("i") InteropLibrary argument) {
        try {
            return argument.asInt(i);
        } catch (UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error");
        }
    }

    @Specialization(guards = {"kind == NUM", "argument.fitsInLong(l)"}, limit ="3")
    public long convertLong(Object l, @CachedLibrary("l") InteropLibrary argument){
        try {
            return argument.asLong(l);
        } catch (UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error");
        }
    }

    @Specialization(guards = {"kind == NUM", "argument.fitsInBigInteger(b)"}, limit ="3")
    public BigInteger convertBigInt(Object b, @CachedLibrary("b") InteropLibrary argument){
        try {
            return argument.asBigInteger(b);
        } catch (UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error");
        }
    }

    @Specialization(guards = {"kind == STRING", "argument.isString(s)"}, limit ="3")
    public TruffleString convertString(Object s, @CachedLibrary("s") InteropLibrary argument){
        try {
            return argument.asTruffleString(s);
        } catch (UnsupportedMessageException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Interop Error", e);
        }
    }

    @Specialization(guards = {"kind == TYPE"})
    public Type convertString(Type t){
        return t;
    }

    @Specialization(guards = {"kind == FUNCTION", "isFunction(f)"})
    public Object convertData(Object f){
        return f;
    }

    @Specialization(guards = {"kind == FUNCTION", "foreignCallable(f, argument)"}, limit ="3")
    public Object convertFunction(Object f,
                                  @CachedLibrary("f") InteropLibrary argument,
                                  @Cached.Shared("handler") @Cached(value = "getDefaultHandler()") ClosureSchemaHandler factory,
                                  @Cached.Shared("argForward") @Cached(value = "argNodes()") NoraNode[] argNodes
    ){
        //We wrap in a closure to be type safe (hide foreign object)
        var sig = typ.applies;
        var caller = new EntryPoint(0, ForeignToNoraConversionNodeGen.create(sig[sig.length-1].type(),new ForeignCall(argNodes,f,argument)));
        return factory.create(caller, typ, f);
    }

    @ExplodeLoop
    @NeverDefault
    protected NoraNode[] argNodes() {
        var sig = typ.applies;
        var argNodes = new NoraNode[sig.length-1];
        CompilerAsserts.partialEvaluationConstant(argNodes.length);
        for(int i = 0; i < argNodes.length; i++){
            argNodes[i] = NoraToForeignConversionNodeGen.create(new ArgNode(i));
        }
        return argNodes;
    }

    @NeverDefault
    protected ClosureSchemaHandler getDefaultHandler(){
        return NoraVmContext.get(null).getSchemaManager().getClosureHandlerFor(Type.NO_GENERICS);
    }

    @Idempotent
    public boolean foreignCallable(Object f, InteropLibrary lib){
        return lib.isExecutable(f) || lib.isInstantiable(f);
    }

    @Idempotent
    protected boolean isFunction(Object f){
        return f instanceof Callable || f instanceof MethodLookup;
    }

    @Specialization(guards = {"kind == DATA", "typ.info.isAssignableFromConcrete(d.typeIndex)"})
    public RuntimeData convertData(RuntimeData d){
        return d;
    }

    @Fallback
    public Object fail(Object f){
        throw new IllegalArgumentException("Type mismatch");
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return ForeignToNoraConversionNodeGen.create(typ, getArgNode());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return typ;
    }
}
