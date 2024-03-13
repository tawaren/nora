package nora.vm.runtime.data;

import com.oracle.truffle.api.interop.TruffleObject;
import com.oracle.truffle.api.strings.TruffleString;
import com.oracle.truffle.api.strings.TruffleStringBuilder;
import nora.vm.types.schemas.utils.SchemaHandler;

public abstract class NoraData implements NoraDataMethods, TruffleObject {
    public final SchemaHandler handler;

    protected NoraData(SchemaHandler handler) {
        this.handler = handler;
    }

    //This makes use from Java and other Languages possible
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof RuntimeData rd){
            if (handler != rd.handler) return false;
            return handler.getUncachedEq().executeDataEq(this, rd);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return handler.getUncachedHashCode().executeHashCode(this);
    }

    @Override
    public String toString() {
        TruffleStringBuilder builder = TruffleStringBuilder.create(TruffleString.Encoding.UTF_16);
        handler.getUncachedToString().executeToString(builder, this);
        return builder.toString();
    }

}
