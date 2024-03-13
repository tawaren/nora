package nora.vm.nodes.arr;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.ExplodeLoop;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.runtime.NoraVmContext;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class CreateObjectArray extends ExecutionNode {
    @Children private NoraNode[] elems;
    private final Type type;

    public CreateObjectArray(NoraNode[] elems, Type type) {
        this.elems = elems;
        this.type = type;
    }

    @ExplodeLoop
    @Override
    public Object execute(VirtualFrame frame) {
        var res = new Object[elems.length+1];
        CompilerAsserts.partialEvaluationConstant(res.length);
        res[0] = type;
        for(int i = 0; i < res.length; i++){
            res[i] = elems[i].execute(frame);
        }
        return res;
    }

    @Override
    public Type getType(SpecFrame frame) {
        return type;
    }

    @Override
    public NoraNode cloneUninitialized() {
        var nElems = new NoraNode[elems.length];
        for(int i = 0; i < nElems.length; i++) nElems[i] = elems[i].cloneUninitialized();
        return new CreateObjectArray(nElems,type);
    }

    @Override
    public int complexity() {
        var count = 1;
        for(int i = 0; i < elems.length; i++) count+=elems[i].complexity();
        return count;
    }

}
