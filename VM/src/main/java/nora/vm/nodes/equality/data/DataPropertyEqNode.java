package nora.vm.nodes.equality.data;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.frame.FrameSlotTypeException;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.equality.EqPlainNode;
import nora.vm.nodes.equality.EqPlainNodeGen;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.IdentityObject;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.TypeUtil;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;
import static nora.vm.types.TypeUtil.*;

@NodeField(name = "reader", type = NoraPropertyGetNode.class)
@NodeField(name = "recursionDepth", type = int.class)
@ImportStatic(TypeKind.class)
@ReportPolymorphism
public abstract class DataPropertyEqNode extends DataEqNode {

    abstract NoraPropertyGetNode getReader();
    abstract int getRecursionDepth();


    //We inline primitives to prevent Boxing
    @Specialization(guards = "kind == BYTE")
    public boolean evalByte(NoraData left, NoraData right, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return getReader().executeByteGet(left) == getReader().executeByteGet(right);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == BOOL")
    public boolean evalBool(NoraData left, NoraData right, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return getReader().executeBooleanGet(left) == getReader().executeBooleanGet(right);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == INT")
    public boolean evalInt(NoraData left, NoraData right, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return getReader().executeIntGet(left) == getReader().executeIntGet(right);
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization(guards = "kind == NUM", rewriteOn = FrameSlotTypeException.class)
    public boolean evalLong(NoraData left, NoraData right, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind)
    throws FrameSlotTypeException {
        try {
            return getReader().executeLongGet(left) == getReader().executeLongGet(right);
        } catch (UnexpectedResultException e){
            throw new FrameSlotTypeException();
        }
    }

    @Specialization(guards = "kind == NUM")
    public boolean evalBig(NoraData left, NoraData right, @Cached.Shared("kind") @Cached("getKind()") TypeKind kind){
        try{
            return getReader().executeBigIntegerGet(left).equals(getReader().executeBigIntegerGet(right));
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Mismatch");
        }
    }

    @Specialization
    public boolean evalPlain(NoraData left, NoraData right, @Cached("getPlainNode()") EqPlainNode eqNode) {
        return eqNode.execute(getReader().executeObjectGet(left), getReader().executeObjectGet(right));
    }

    @NeverDefault
    protected TypeKind getKind(){
        var util = NoraVmContext.getTypeUtil(null);
        return util.getKind(getReader().getPropertyType().info);
    }

    @NeverDefault
    protected EqPlainNode getPlainNode(){
        return EqPlainNodeGen.create(getRecursionDepth()+1);
    }

    public DataPropertyEqNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return DataPropertyEqNodeGen.create(getReader().cloneUninitialized(), getRecursionDepth());
    }
}
