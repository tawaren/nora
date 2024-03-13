package nora.vm.nodes.property.reader;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.CompilerDirectives.CompilationFinal;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.Type;

import java.math.BigInteger;

import static com.oracle.truffle.api.CompilerDirectives.transferToInterpreterAndInvalidate;

public class NumericPropertyGetNode extends NoraPropertyGetNode {
    private final StaticProperty longProp;
    private final StaticProperty bigProp;
    @CompilationFinal boolean bigSeen = false;


    public NumericPropertyGetNode(StaticProperty longProp, StaticProperty bigProp) {
        this.longProp = longProp;
        this.bigProp = bigProp;
    }

    @Override
    public long executeLongGet(NoraData data) throws UnexpectedResultException {
        if(bigSeen) {
            var big = bigProp.getObject(data);
            if(big == null) return longProp.getLong(data);
            throw new UnexpectedResultException(big);
        } else {
            var l = longProp.getLong(data);
            if(l != 0) return l;
            var big = bigProp.getObject(data);
            if(big == null) return l;
            transferToInterpreterAndInvalidate();
            bigSeen = true;
            throw new UnexpectedResultException(big);
        }
    }

    @Override
    public BigInteger executeBigIntegerGet(NoraData data) {
        var big = bigProp.getObject(data);
        if(big instanceof BigInteger bi) return bi;
        return BigInteger.valueOf(longProp.getLong(data));
    }

    @Override
    public Object executeObjectGet(NoraData data) {
        var big = bigProp.getObject(data);
        if(big == null) return longProp.getLong(data);
        return big;
    }

    @Override
    public Object executeGenericGet(NoraData data) {
        if(bigSeen) {
            var big = bigProp.getObject(data);
            if(big == null) return longProp.getLong(data);
            return big;
        } else {
            var l = longProp.getLong(data);
            if(l != 0) return l;
            var big = bigProp.getObject(data);
            if(big == null) return l;
            transferToInterpreterAndInvalidate();
            bigSeen = true;
            return big;
        }
    }

    @Override
    public NoraPropertyGetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new NumericPropertyGetNode(bigProp, longProp);
    }

    @Override
    public Type getPropertyType() {
        return NoraVmContext.getTypeUtil(this).NumType;
    }
}
