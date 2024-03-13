package nora.vm.nodes;



import com.oracle.truffle.api.CompilerAsserts;
import com.oracle.truffle.api.dsl.*;
import com.oracle.truffle.api.nodes.UnexpectedResultException;
import nora.vm.nodes.property.reader.NoraPropertyGetNode;
import nora.vm.runtime.data.RuntimeData;
import nora.vm.specTime.SpecFrame;
import nora.vm.types.Type;
import nora.vm.types.TypeUtil;
import nora.vm.types.schemas.DataSchemaHandler;
import nora.vm.types.schemas.utils.SchemaHandler;

import java.math.BigInteger;

@ImportStatic({TypeUtil.TypeKind.class, TypeUtil.class})
@NodeChild(value = "target",  type = NoraNode.class)
@ReportPolymorphism
public abstract class FieldNode extends ExecutionNode {
    protected static int INLINE_CACHE_SIZE = 3;
    abstract NoraNode getTarget();
    private final int slot;
    protected final Type typ;
    protected final TypeUtil.TypeKind kind;

    protected FieldNode(int slot, Type typ, TypeUtil.TypeKind kind) {
        this.slot = slot;
        this.typ = typ;
        this.kind = kind;
    }

    //This assumes optimistically that the same comes along or a subtype - which is true as long no variant types are around
    @Specialization(rewriteOn = {UnexpectedResultException.class, IllegalArgumentException.class}, guards = {"kind == BOOL"})
    public boolean getSuperBoolField(RuntimeData data, @Cached(value = "getGetter(data.handler)", neverDefault = true) NoraPropertyGetNode getter)
            throws UnexpectedResultException {
        return getter.executeBooleanGet(data);
    }

    @Specialization(rewriteOn = UnexpectedResultException.class, guards = {"kind == BOOL", "data.handler == cachedHandler"}, limit = "INLINE_CACHE_SIZE")
    public boolean getBoolField(RuntimeData data,
                           @Cached("data.handler") SchemaHandler cachedHandler,
                           @Cached("getGetter(cachedHandler)") NoraPropertyGetNode getter
    ) throws UnexpectedResultException {
        return getter.executeBooleanGet(data);
    }

    //This assumes optimistically that the same comes along or a subtype - which is true as long no variant types are around
    @Specialization(rewriteOn = {UnexpectedResultException.class, IllegalArgumentException.class}, guards = {"kind == INT"})
    public int getSuperIntField(RuntimeData data, @Cached(value = "getGetter(data.handler)", neverDefault = true) NoraPropertyGetNode getter)
            throws UnexpectedResultException {
        return getter.executeIntGet(data);
    }

    @Specialization(rewriteOn = UnexpectedResultException.class, guards = {"kind == INT", "data.handler == cachedHandler"}, limit = "INLINE_CACHE_SIZE")
    public int getIntField(RuntimeData data,
                           @Cached("data.handler") SchemaHandler cachedHandler,
                           @Cached("getGetter(cachedHandler)") NoraPropertyGetNode getter
    ) throws UnexpectedResultException {
        return getter.executeIntGet(data);
    }

    //This assumes optimistically that the same comes along or a subtype - which is true as long no variant types are around
    @Specialization(rewriteOn = {UnexpectedResultException.class, IllegalArgumentException.class}, guards = {"kind == NUM"})
    public long getSuperNumLongField(RuntimeData data, @Cached(value = "getGetter(data.handler)", neverDefault = true) NoraPropertyGetNode getter)
            throws UnexpectedResultException {
        return getter.executeLongGet(data);
    }

    @Specialization(rewriteOn = UnexpectedResultException.class, guards = {"kind == NUM", "data.handler == cachedHandler"}, limit = "INLINE_CACHE_SIZE")
    public long getNumLongField(RuntimeData data,
                           @Cached("data.handler") SchemaHandler cachedHandler,
                           @Cached("getGetter(cachedHandler)") NoraPropertyGetNode getter
    ) throws UnexpectedResultException {
        return getter.executeLongGet(data);
    }

    @Specialization(rewriteOn = UnexpectedResultException.class, replaces = "getNumLongField", guards = {"kind == NUM", "data.handler == cachedHandler"}, limit = "INLINE_CACHE_SIZE")
    public BigInteger getNumBigField(RuntimeData data,
                                     @Cached("data.handler") SchemaHandler cachedHandler,
                                     @Cached("getGetter(cachedHandler)") NoraPropertyGetNode getter
    ) throws UnexpectedResultException {
        return getter.executeBigIntegerGet(data);
    }

    //This assumes optimistically that the same comes along or a subtype - which is true as long no variant types are around
    @Specialization(rewriteOn = IllegalArgumentException.class, guards = {"isObject(kind)"})
    public Object getSuperObjectField(RuntimeData data, @Cached(value = "getGetter(data.handler)", neverDefault = true) NoraPropertyGetNode getter) {
        return getter.executeObjectGet(data);
    }

    @Specialization(guards = {"isObject(kind)", "data.handler == cachedHandler"}, limit = "INLINE_CACHE_SIZE")
    public Object getObjectField(RuntimeData data,
                                    @Cached("data.handler") SchemaHandler cachedHandler,
                                    @Cached("getGetter(cachedHandler)") NoraPropertyGetNode getter
    ) {
        return getter.executeObjectGet(data);
    }

    @Specialization(
            rewriteOn = {IllegalArgumentException.class},
            replaces = {"getSuperBoolField", "getBoolField", "getSuperIntField", "getIntField", "getNumBigField", "getSuperObjectField", "getObjectField"}
    )
    public Object getSuperGenericField(RuntimeData data,
                                       @Cached(value = "getGetter(data.handler)", neverDefault = true) NoraPropertyGetNode getter) {
        return getter.executeGenericGet(data);
    }

    @Specialization(
        replaces = {"getSuperGenericField"},
        guards = {"data.handler == cachedHandler"},
        limit = "INLINE_CACHE_SIZE"
    )
    public Object getGenericField(RuntimeData data,
                                    @Cached("data.handler") SchemaHandler cachedHandler,
                                    @Cached("getGetter(cachedHandler)") NoraPropertyGetNode getter
    )  {
        //This can happen if higher language places primitives into a hierarchy
        return getter.executeGenericGet(data);
    }

    @Specialization(replaces = {"getGenericField"})
    public Object getGenericFieldSlow(RuntimeData data) {
        return getGetter(data.handler).executeGenericGet(data);
    }

    protected NoraPropertyGetNode getGetter(SchemaHandler cachedHandler){
        var getter = cachedHandler.getProperty(slot).getGetter();
        assert getter.getPropertyType().subTypeOf(typ);
        return getter;
    }


    @Override
    public String toString() {
        return getTarget()+"["+slot+"]:"+typ;
    }

    @Override
    public FieldNode cloneUninitialized() {
        CompilerAsserts.neverPartOfCompilation();
        return FieldNodeGen.create(slot,typ,kind,getTarget());
    }

    @Override
    public Type getType(SpecFrame frame) {
        CompilerAsserts.neverPartOfCompilation();
        return typ;
    }
}
