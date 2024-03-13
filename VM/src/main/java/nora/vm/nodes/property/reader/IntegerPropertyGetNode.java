package nora.vm.nodes.property.reader;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.Type;

import java.math.BigInteger;

public class IntegerPropertyGetNode extends NoraPropertyGetNode {
    private final StaticProperty property;

    public IntegerPropertyGetNode(StaticProperty property) {
        this.property = property;
    }

    @Override
    public int executeIntGet(NoraData data) throws UnexpectedResultException {
        return property.getInt(data);
    }

    @Override
    public long executeLongGet(NoraData data) throws UnexpectedResultException {
        return property.getInt(data);
    }

    @Override
    public BigInteger executeBigIntegerGet(NoraData data) throws UnexpectedResultException {
        return BigInteger.valueOf(property.getInt(data));
    }

    @Override
    public Object executeObjectGet(NoraData data) {
        return property.getInt(data);
    }

    @Override
    public Object executeGenericGet(NoraData data) {
        return property.getInt(data);
    }

    @Override
    public NoraPropertyGetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new IntegerPropertyGetNode(property);
    }

    @Override
    public Type getPropertyType() {
        return NoraVmContext.getTypeUtil(this).IntType;
    }
}
