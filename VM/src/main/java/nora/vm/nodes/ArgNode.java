package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.strings.TruffleString;
import nora.vm.nodes.consts.ConstNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

public class ArgNode extends NoraNode{
    public final int slot;

    public ArgNode(int slot) {
        this.slot = slot;
    }

    @Override
    public boolean executeBoolean(VirtualFrame frame) throws UnexpectedResultException {
        return (Boolean) frame.getArguments()[slot];
    }

    @Override
    public byte executeByte(VirtualFrame frame) throws UnexpectedResultException {
        return (Byte) frame.getArguments()[slot];
    }

    @Override
    public int executeInt(VirtualFrame frame) throws UnexpectedResultException {
        return (Integer) frame.getArguments()[slot];
    }

    @Override
    public long executeLong(VirtualFrame frame) throws UnexpectedResultException {
        return (Long) frame.getArguments()[slot];
    }

    @Override
    public Object execute(VirtualFrame frame)  {
        return frame.getArguments()[slot];
    }

    @Override
    public NoraNode specialise(SpecFrame frame) throws Exception {
        CompilerAsserts.neverPartOfCompilation();
        if(frame.isKnownArgumentVal(slot)){
            var arg = frame.getArgumentVal(slot);
            return ConstNode.create(arg);
        } else {
            return new ArgNode(slot);
        }
    }

    @Override
    public String toString() {
        return "Arg("+slot+")";
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return this;
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        assert frame.isKnownArgumentTyp(slot);
        return frame.getArgumentTyp(slot);
    }

    @Override
    public boolean isUnboxed() {
        return false;
    }
}
