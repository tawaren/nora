package nora.compiler.entries.interfaces;

import nora.compiler.entries.Instance;

import java.util.Set;

public interface WithTraits {
    Set<Instance> getTraits();
    //Just used to find it, type-safety is checked over instances
    boolean hasTrait(Trait trait);
}
