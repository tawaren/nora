package nora.vm.nodes;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.runtime.data.ClosureData;

import java.math.BigInteger;

//Todo: Later have a RuntimeDataRepr that supports unboxed storage
public class CaptureNode extends ExecutionNode{
    @Child NoraPropertyGetNode getter;

    public CaptureNode(NoraPropertyGetNode getter) {
        this.getter = getter;
    }

    @Override
    public boolean executeBoolean(VirtualFrame virtualFrame) throws UnexpectedResultException {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeBooleanGet(data);
    }

    @Override
    public byte executeByte(VirtualFrame virtualFrame) throws UnexpectedResultException {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeByteGet(data);
    }

    @Override
    public int executeInt(VirtualFrame virtualFrame) throws UnexpectedResultException {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeIntGet(data);
    }

    @Override
    public long executeLong(VirtualFrame virtualFrame) throws UnexpectedResultException {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeLongGet(data);
    }

    @Override
    public BigInteger executeBigInteger(VirtualFrame virtualFrame) throws UnexpectedResultException {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeBigIntegerGet(data);
    }

    @Override
    public Object executeObject(VirtualFrame virtualFrame) throws UnexpectedResultException {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeObjectGet(data);
    }

    @Override
    public Object execute(VirtualFrame virtualFrame) {
        ClosureData data = (ClosureData)virtualFrame.getArguments()[0];
        return getter.executeGenericGet(data);
    }


    @Override
    public String toString() {
        return getter.toString();
    }

    @Override
    public CaptureNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new CaptureNode(getter.cloneUninitialized());
    }

    @Override
    public int complexity() {
        return 2;
    }

}
