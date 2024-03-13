package nora.vm.types.pattern;

import nora.vm.types.Type;
import nora.vm.types.TypeInfo;

public class SimpleTypePattern implements TypePattern{
    private final TypeInfo info;
    private final Type.Variance variance;

    public SimpleTypePattern(TypeInfo info) {
        this.info = info;
        this.variance = null;
    }

    public SimpleTypePattern(TypeInfo info, Type.Variance variance) {
        this.info = info;
        this.variance = variance;
    }

    @Override
    public Type buildType(Type[] matches) {
        return new Type(info.stabilized(), Type.NO_APPLIES);
    }

    @Override
    public Type.TypeParameter buildTypeParam(Type[] matches) {
        assert variance != null;
        return new Type.TypeParameter(variance,buildType(matches));
    }

    @Override
    public MixedResult mixedDynamicArgTypeMatch(Type typ, Type[] matches) {
        assert variance == null;
        if(typ.info.subTypeOf(info)) return MixedResult.Super;
        if(info.subTypeOf(typ.info)) return MixedResult.Sub;
        return MixedResult.None;
    }

    @Override
    public boolean mixedTypeMatch(Type typ, Type[] matches) {
        if(variance == Type.Variance.Inv) return fixedMatch(typ, matches);
        return typ.info.subTypeOf(info) || info.subTypeOf(typ.info);
    }

    @Override
    public boolean subtypeMatch(Type typ, Type[] matches) {
        if(variance == Type.Variance.Inv) return fixedMatch(typ, matches);
        if(variance == Type.Variance.Contra) return typ.info.subTypeOf(info);
        return info.subTypeOf(typ.info);
    }

    @Override
    public boolean supertypeMatch(Type typ, Type[] matches) {
        if(variance == Type.Variance.Inv) return fixedMatch(typ, matches);
        if(variance == Type.Variance.Contra) return info.subTypeOf(typ.info);
        return typ.info.subTypeOf(info);
    }

    @Override
    public boolean fixedMatch(Type typ, Type[] matches) {
        return typ.info.equals(info);
    }

    @Override
    public Type.Variance getVariance() {
        return variance;
    }

    @Override
    public TypePattern withVariance(Type.Variance variance) {
        return new SimpleTypePattern(info, variance);
    }

    public TypeInfo getInfo() {
        return info;
    }

    @Override
    public String toString() {
        return info.toString();
    }
}
