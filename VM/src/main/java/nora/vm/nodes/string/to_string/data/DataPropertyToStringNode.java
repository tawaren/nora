package nora.vm.nodes.string.to_string.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.equality.EqPlainNode;
import nora.vm.nodes.equality.EqPlainNodeGen;
import nora.vm.nodes.equality.data.DataEqNode;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.nodes.string.to_string.ToStringPlainNode;
import nora.vm.nodes.string.to_string.ToStringPlainNodeGen;
import nora.vm.nodes.string.to_string.data.DataPropertyToStringNodeGen;
import nora.vm.nodes.string.to_string.data.DataToStringRecursionNodeGen;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.TypeUtil;
import nora.vm.runtime.data.RuntimeData;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.types.Type;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;
import static nora.vm.types.TypeUtil.*;

@NodeField(name = "reader", type = NoraPropertyGetNode.class)
@NodeField(name = "recursionDepth", type = int.class)
@ImportStatic(TypeKind.class)
@ReportPolymorphism
public abstract class DataPropertyToStringNode extends DataToStringNode {

    abstract NoraPropertyGetNode getReader();
    abstract int getRecursionDepth();


    //We inline primitives to prevent Boxing
    @Specialization(guards = "kind == BYTE")
    public void evalByte(TruffleStringBuilder builder, NoraData val,
                            @Cached TruffleStringBuilder.AppendByteNode appender,
                            @Cached.Shared("kind") @Cached("getKind()") TypeKind kind) {
        try{
            appender.execute(builder,getReader().executeByteGet(val));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == BOOL")
    public void evalBool(TruffleStringBuilder builder, NoraData val,
                         @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node appender,
                         @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            if(getReader().executeBooleanGet(val)) appender.execute(builder, "true");
            else appender.execute(builder, "false");
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == INT")
    public void evalInt(TruffleStringBuilder builder, NoraData val,
                           @Cached TruffleStringBuilder.AppendIntNumberNode appender,
                           @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
             appender.execute(builder, getReader().executeIntGet(val));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = FrameSlotTypeException.class)
    public void evalLong(TruffleStringBuilder builder, NoraData val,
                            @Cached TruffleStringBuilder.AppendLongNumberNode appender,
                            @Cached.Shared("kind") @Cached("getKind()") TypeKind kind)
            throws FrameSlotTypeException {
        try {
            appender.execute(builder, getReader().executeLongGet(val));
        } catch (UnexpectedResultException e){
            throw new FrameSlotTypeException();
        }
    }

    @Specialization(guards = "kind == NUM")
    public void evalBig(TruffleStringBuilder builder, NoraData val,
                        @Cached.Shared("appendJava") @Cached TruffleStringBuilder.AppendJavaStringUTF16Node appender,
                        @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            appender.execute(builder, getReader().executeBigIntegerGet(val).toString());
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization
    public void evalPlain(TruffleStringBuilder builder, NoraData val, @Cached("getPlainNode()") ToStringPlainNode toStringNode) {
        toStringNode.execute(builder, getReader().executeObjectGet(val));
    }

    @NeverDefault
    protected TypeKind getKind(){
        var util = NoraVmContext.getTypeUtil(null);
        return util.getKind(getReader().getPropertyType().info);
    }

    @NeverDefault
    protected ToStringPlainNode getPlainNode(){
        return ToStringPlainNodeGen.create(getRecursionDepth()+1);
    }

    public DataPropertyToStringNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DataPropertyToStringNodeGen.create(getReader().cloneUninitialized(), getRecursionDepth());
    }
}
