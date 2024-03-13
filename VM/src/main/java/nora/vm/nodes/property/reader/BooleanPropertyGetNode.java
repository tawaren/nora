package nora.vm.nodes.property.reader;

import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import com.oracle.truffle.api.staticobject.StaticProperty;
import nora.vm.runtime.NoraVmContext;
import nora.vm.runtime.data.NoraData;
import nora.vm.types.Type;

import java.math.BigInteger;

public class BooleanPropertyGetNode extends NoraPropertyGetNode {
    private final StaticProperty property;

    public BooleanPropertyGetNode(StaticProperty property) {
        this.property = property;
    }

    @Override
    public boolean executeBooleanGet(NoraData data) {
        return property.getBoolean(data);
    }

    @Override
    public Object executeObjectGet(NoraData data){
        return property.getBoolean(data);
    }

    @Override
    public Object executeGenericGet(NoraData data) {
        return property.getBoolean(data);
    }

    @Override
    public NoraPropertyGetNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return new BooleanPropertyGetNode(property);
    }

    @Override
    public Type getPropertyType() {
        return NoraVmContext.getTypeUtil(this).BooleanType;
    }
}
