package nora.vm.nodes.arr;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

import javax.swing.event.RowSorterEvent;
import java.math.BigInteger;
import java.util.BitSet;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class CreatePrimitiveArray extends ExecutionNode {
    @Children private NoraNode[] elems;
    private final TypeUtil.TypeKind kind;
    @CompilationFinal private Type numType = null;

    public CreatePrimitiveArray(NoraNode[] elems, TypeUtil.TypeKind kind) {
        this.elems = elems;
        this.kind = kind;
    }

    @Override
    public Object execute(VirtualFrame frame) {
        return switch (kind){
            case BOOL -> createBoolArray(frame);
            case BYTE -> createByteArray(frame);
            case INT -> createIntArray(frame);
            case NUM -> {
                if(numType != null){
                    yield createBigArray(frame);
                } else {
                    yield createLongArray(frame);
                }
            }
            case STRING, FUNCTION, ARRAY, DATA, TYPE -> throw new RuntimeException("Not a primitive array type");
        };
    }

    @ExplodeLoop
    private boolean[] createBoolArray(VirtualFrame frame){
        try{
            var res = new boolean[elems.length];
            CompilerAsserts.partialEvaluationConstant(res.length);
            for (int i = 0; i < res.length; i++){
                res[i] = elems[i].executeBoolean(frame);
            }
            return res;
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Missmatch");
        }
    }
    @ExplodeLoop
    private byte[] createByteArray(VirtualFrame frame){
        try{
            var res = new byte[elems.length];
            CompilerAsserts.partialEvaluationConstant(res.length);
            for (int i = 0; i < res.length; i++){
                res[i] = elems[i].executeByte(frame);
            }
            return res;
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Missmatch");
        }
    }
    @ExplodeLoop
    private int[] createIntArray(VirtualFrame frame){
        try{
            var res = new int[elems.length];
            CompilerAsserts.partialEvaluationConstant(res.length);
            for (int i = 0; i < res.length; i++){
                res[i] = elems[i].executeInt(frame);
            }
            return res;
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Missmatch");
        }
    }
    @ExplodeLoop
    private Object createLongArray(VirtualFrame frame){
        var res = new long[elems.length];
        int i = 0;
        try{
            CompilerAsserts.partialEvaluationConstant(res.length);
            for (; i < res.length; i++){
                res[i] = elems[i].executeLong(frame);
            }
            return res;
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            var big = e.getResult();
            if(big instanceof BigInteger bi) return transition(frame, i, res, bi);
            throw new RuntimeException("Type Missmatch");
        }
    }

    private Object[] transition(VirtualFrame frame, int evalPos, long[] partial, BigInteger next){
        CompilerAsserts.neverPartOfCompilation();
        numType = NoraVmContext.getTypeUtil(null).NumArrayType;
        var res = new Object[elems.length+1];
        res[0] = numType;
        for(int i = 0; i <= evalPos; i++){
            res[i+1] = partial[i];
        }
        res[evalPos+1] = next;
        try{
            for(int i = evalPos+1; i < res.length; i++){
                res[i+1] = elems[i].executeBigInteger(frame);
            }
            return res;
        } catch (UnexpectedResultException e) {
            throw new RuntimeException("Type Missmatch");
        }
    }

    @ExplodeLoop
    private Object[] createBigArray(VirtualFrame frame){
        try{
            var res = new Object[elems.length+1];
            res[0] = numType;
            CompilerAsserts.partialEvaluationConstant(res.length);
            for(int i = 0; i < res.length; i++){
                res[i] = elems[i].executeBigInteger(frame);
            }
            return res;
        } catch (UnexpectedResultException e) {
            transferToInterpreterAndInvalidate();
            throw new RuntimeException("Type Missmatch");
        }
    }

    @Override
    public Type getType(SpecFrame frame) {
        var util = NoraVmContext.getTypeUtil(null);
        return switch (kind){
            case BOOL -> util.BooleanArrayType;
            case BYTE -> util.ByteArrayType;
            case INT -> util.IntArrayType;
            case NUM -> util.NumArrayType;
            case STRING, FUNCTION, ARRAY, DATA, TYPE -> null; //Not Primitive
        };
    }

    @Override
    public NoraNode cloneUninitialized() {
        var nElems = new NoraNode[elems.length];
        for(int i = 0; i < nElems.length; i++) nElems[i] = elems[i].cloneUninitialized();
        return new CreatePrimitiveArray(nElems,kind);
    }

    @Override
    public int complexity() {
        var count = 1;
        for(int i = 0; i < elems.length; i++) count+=elems[i].complexity();
        return count;
    }
}
