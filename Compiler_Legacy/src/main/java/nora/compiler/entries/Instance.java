package nora.compiler.entries;

import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.resolved.Variance;

import java.util.*;

//Todo: How to mark a generic one???
//      do we need an interface with 2 subtypes
//      or a Generic Definition? or null as definition?
public interface Instance {
    //Processes
    boolean validateAndInfer(Variance variance);

    //Type Checks
    boolean fulfills(Instance other);
    boolean hasTrait(Instance trait);
    boolean subType(Instance other);
    Instance substitute(List<Instance> generics);
    void match(Instance match, Instance[] extractedGenerics);

    //Structural Processes
    Instance findSuperWithTrait(Trait trait);
    void collectDataHierarchy(LinkedHashSet<Instance> hierarchy);
    void collectTraitHierarchy(LinkedHashSet<Instance> hierarchy);

    static LinkedHashSet<Instance> collectDataHierarchy(Instance inst){
        LinkedHashSet<Instance> hierarchy = new LinkedHashSet<>();
        inst.collectDataHierarchy(hierarchy);
        return hierarchy;
    }

    static LinkedHashSet<Instance> collectTraitHierarchy(Instance inst){
        LinkedHashSet<Instance> hierarchy = new LinkedHashSet<>();
        inst.collectTraitHierarchy(hierarchy);
        return hierarchy;
    }

    //Categorisation
    boolean isFunction();
    boolean isMultiMethod();
    default boolean isCallable(){
        return isFunction() || isMultiMethod();
    }
    default boolean isParametric(){
        return isFunction() || isMultiMethod() || isData() || isTrait();
    }
    default boolean isWithTraits(){
        return isTrait() || isData();
    }
    default boolean isWithArguments(){
        return isCallable() || isData();
    }

    boolean isTrait();
    boolean isData();
    boolean isGeneric();

}
