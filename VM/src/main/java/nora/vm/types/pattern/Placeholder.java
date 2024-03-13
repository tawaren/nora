package nora.vm.types.pattern;

import nora.vm.types.Type;

public class Placeholder implements TypePattern{
    private final Type.Variance variance;
    private final int index;

    public Placeholder(int index) {
        //meaning in a position where it should be irrelevant
        variance = null;
        this.index = index;
    }

    private Placeholder(int index, Type.Variance variance) {
        this.variance = variance;
        this.index = index;
    }

    @Override
    public Type buildType(Type[] matches) {
        return matches[index];
    }

    @Override
    public Type.TypeParameter buildTypeParam(Type[] matches) {
        assert variance != null;
        return new Type.TypeParameter(variance,buildType(matches));
    }

    @Override
    public boolean mixedTypeMatch(Type typ, Type[] matches) {
        //Placeholder Matches are always invariant
        return fixedMatch(typ, matches);
    }

    @Override
    public MixedResult mixedDynamicArgTypeMatch(Type typ, Type[] matches) {
        assert variance == null;
        var res = fixedMatch(typ, matches);
        if(res) return MixedResult.Super;
        return MixedResult.None;
    }

    @Override
    public boolean subtypeMatch(Type typ, Type[] matches) {
        //Placeholder Matches are always invariant
        return fixedMatch(typ, matches);
    }

    @Override
    public boolean supertypeMatch(Type typ, Type[] matches) {
        //Placeholder Matches are always invariant
        return fixedMatch(typ, matches);
    }

    @Override
    public boolean fixedMatch(Type typ, Type[] matches) {
        if(matches[index] == null) matches[index] = typ;
        return matches[index].sameType(typ);
    }


    @Override
    public Type.Variance getVariance() {
        return variance;
    }

    @Override
    public TypePattern withVariance(Type.Variance variance) {
        return new Placeholder(index, variance);
    }

    @Override
    public String toString() {
        return "?"+index;
    }
}
