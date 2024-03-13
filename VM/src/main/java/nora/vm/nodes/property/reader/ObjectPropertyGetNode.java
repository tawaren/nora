package nora.vm.nodes.property.reader;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.Type;

import java.math.BigInteger;

public class ObjectPropertyGetNode extends NoraPropertyGetNode {
    private final StaticProperty property;
    private final Type type;

    public ObjectPropertyGetNode(StaticProperty property, Type type) {
        this.property = property;
        this.type = type;
    }

    @Override
    public BigInteger executeBigIntegerGet(NoraData data) throws UnexpectedResultException {
        var res = property.getObject(data);
        if(res instanceof BigInteger bi) return bi;
        throw new UnexpectedResultException(res);
    }

    @Override
    public Object executeObjectGet(NoraData data) {
        return property.getObject(data);
    }

    @Override
    public Object executeGenericGet(NoraData data) {
        return property.getObject(data);
    }

    @Override
    public NoraPropertyGetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new ObjectPropertyGetNode(property, type);
    }

    @Override
    public Type getPropertyType() {
        return type;
    }
}
