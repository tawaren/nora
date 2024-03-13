package nora.vm.nodes.property.reader;


import com.oracle.truffle.api.dsl.Idempotent;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.runtime.data.NoraData;

import nora.vm.types.Type;

import java.math.BigInteger;

public abstract class NoraPropertyGetNode extends Node {

    public abstract Object executeObjectGet(NoraData data);
    public int executeIntGet(NoraData data) throws UnexpectedResultException {
        throw new UnexpectedResultException(executeObjectGet(data));
    }
    public byte executeByteGet(NoraData data) throws UnexpectedResultException {
        throw new UnexpectedResultException(executeObjectGet(data));
    };
    public boolean executeBooleanGet(NoraData data) throws UnexpectedResultException {
        throw new UnexpectedResultException(executeObjectGet(data));
    };
    public long executeLongGet(NoraData data) throws UnexpectedResultException {
        throw new UnexpectedResultException(executeObjectGet(data));
    };
    public BigInteger executeBigIntegerGet(NoraData data) throws UnexpectedResultException {
        throw new UnexpectedResultException(executeObjectGet(data));
    };

    //Different to object caller does not know what it will be
    public abstract Object executeGenericGet(NoraData data);
    public abstract NoraPropertyGetNode cloneUninitialized();

    //Extracts the field type
    @Idempotent
    public abstract Type getPropertyType();
}
