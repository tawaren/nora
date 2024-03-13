package nora.vm.types.pattern;

import nora.vm.types.Type;

public interface TypePattern {

    Type.Variance getVariance();
    TypePattern withVariance(Type.Variance variance);

    enum MixedResult {
        Super, Sub, None
    }

    //Super takes precedence (in case of exact match)
    MixedResult mixedDynamicArgTypeMatch(Type typ, Type[] matches);
    boolean mixedTypeMatch(Type typ, Type[] matches);
    boolean subtypeMatch(Type typ, Type[] matches);
    boolean supertypeMatch(Type typ, Type[] matches);
    boolean fixedMatch(Type typ, Type[] matches);

    Type buildType(Type[] matches);

    Type.TypeParameter buildTypeParam(Type[] matches);

}
