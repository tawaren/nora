package nora.vm.nodes.hash_code.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.hash_code.HashCodePlainNode;
import nora.vm.nodes.hash_code.HashCodePlainNodeGen;
import nora.vm.nodes.hash_code.NumHashCode;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;

import static nora.vm.types.TypeUtil.*;

@NodeField(name = "reader", type = NoraPropertyGetNode.class)
@NodeField(name = "recursionDepth", type = int.class)
@ImportStatic(TypeKind.class)
@ReportPolymorphism
public abstract class DataPropertyHashCodeNode extends DataHashCodeNode {

    abstract NoraPropertyGetNode getReader();
    abstract int getRecursionDepth();

    //We inline primitives to prevent Boxing
    @Specialization(guards = "kind == BYTE")
    public int evalByte(NoraData left, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return Byte.hashCode(getReader().executeByteGet(left));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == BOOL")
    public int evalBool(NoraData left, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return Boolean.hashCode(getReader().executeBooleanGet(left));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == INT")
    public int evalInt(NoraData left, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return Integer.hashCode(getReader().executeIntGet(left));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = FrameSlotTypeException.class)
    public int evalLong(NoraData left, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind)
            throws FrameSlotTypeException {
        try {
            return NumHashCode.hashCode(getReader().executeLongGet(left));
        } catch (UnexpectedResultException e){
            throw new FrameSlotTypeException();
        }
    }

    @Specialization(guards = "kind == NUM")
    public int evalBig(NoraData left, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return NumHashCode.hashCode(getReader().executeBigIntegerGet(left));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization
    public int evalPlain(NoraData left, @Cached("getPlainNode()") HashCodePlainNode hashNode) {
        return hashNode.execute(getReader().executeObjectGet(left));
    }

    @NeverDefault
    protected TypeKind getKind(){
        var util = NoraVmContext.getTypeUtil(null);
        return util.getKind(getReader().getPropertyType().info);
    }

    @NeverDefault
    protected HashCodePlainNode getPlainNode(){
        return HashCodePlainNodeGen.create(getRecursionDepth()+1);
    }

    @Idempotent
    protected boolean isIdentityObject(Object obj){
        return obj instanceof IdentityObject;
    }

    public DataPropertyHashCodeNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DataPropertyHashCodeNodeGen.create(getReader().cloneUninitialized(), getRecursionDepth());
    }
}
