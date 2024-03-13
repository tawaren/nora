package nora.vm.nodes.interop;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.NodeChild;
import com.oracle.truffle.api.dsl.Specialization;
import nora.vm.nodes.ExecutionNode;
import nora.vm.nodes.NoraNode;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;

import java.math.BigInteger;

@NodeChild(value = "argNode", type = NoraNode.class)
public abstract class NoraToForeignConversionNode extends ExecutionNode {
    abstract NoraNode getArgNode();

    @Specialization
    public boolean passBoolean(boolean b) {
        return b;
    }

    @Specialization
    public byte passByte(byte b) {
        return b;
    }

    @Specialization
    public int passInt(int i) {
        return i;
    }

    @Specialization
    public long passLong(long l) {
        return l;
    }

    @Specialization
    public Object convertBig(BigInteger bi) {
        return new BigWrapper(bi);
    }


    @Specialization
    public Object passObject(Object o) {
        return o;
    }

    @Override
    public NoraNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return NoraToForeignConversionNodeGen.create(getArgNode());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return getArgNode().getType(frame);
    }
}
