package nora.compiler.entries;

import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;
import nora.compiler.processing.TypeCheckContext;

import java.util.*;
import java.util.stream.Collectors;

//Todo: How to mark a generic one???
//      do we need an interface with 2 subtypes
//      or a Generic Definition? or null as definition?
public class DefInstance implements Instance {
    private final Definition base;
    private final List<Instance> arguments;

    public DefInstance(Definition base, List<Instance> arguments) {
        this.base = base;
        this.arguments = arguments;
    }

    public DefInstance(Definition base) {
        this(base, null);
    }

    private Boolean valid = null;
    private boolean cacheValidationRes(boolean res){
        if(!res) throw new RuntimeException("Validation failed"); //Better for now
        valid = res;
        return res;
    }

    public boolean validateAndInfer(Variance position) {
        if(valid != null) return valid;
        //cycle prevention (should not be possible but better safe than sorry)
        valid = false;
        var para = base.asParametric();
        if(para == null) return cacheValidationRes(false);
        if(arguments == null) return cacheValidationRes(false);
        //We fall back to contravariance on Bivariance type args
        // Bivariance is only permitted on the argument itself
        if(position == Variance.Bivariance) position = Variance.Contravariance;
        if(!para.validateApplication(arguments, position)) return cacheValidationRes(false);
        var allTraits = Instance.collectTraitHierarchy(this);
        var allTraitNames = allTraits.stream().map(ti -> ((DefInstance)ti).getBase().getFullyQualifiedName()).collect(Collectors.toSet());
        if(allTraits.size() != allTraitNames.size()) return cacheValidationRes(false);
        return cacheValidationRes(true);
    }

    @Override
    public boolean fulfills(Instance other){
        if(subType(other)) return true;
        return hasTrait(other);
    }

    @Override
    public boolean hasTrait(Instance trait) {
        if(equals(trait)) return true;
        if(!trait.isTrait()) return false;
        if(isWithTraits()){
            return base.asWithTraits().getTraits().stream()
                    .map(t -> t.substitute(arguments))
                    .anyMatch(t -> t.hasTrait(trait));
        } else {
            return false;
        }
    }

    @Override
    public Instance findSuperWithTrait(Trait trait) {
        if(!isData()) return null;
        var data = base.asData();
        if(data.hasTrait(trait)) return this;
        if(data.getParent() == null) return null;
        return new DefInstance(data.getParent(), arguments).findSuperWithTrait(trait);
    }

    private boolean argSubType(DefInstance other){
        List<Variance> variances = null;
        if(TypeCheckContext.isSpecialVarargDef(getBase())){
            variances = TypeCheckContext.getVarargVariance(getBase(), arguments);
        } else {
            variances = getBase().asParametric().getGenerics().stream().map(Generic::variance).toList();
        }
        if(variances.size() != arguments.size()) return false;
        if(variances.size() != other.arguments.size()) return false;
        var varIt = variances.iterator();
        var argIt = arguments.iterator();
        var otherArgIt = other.arguments.iterator();
        while (varIt.hasNext() && argIt.hasNext() && otherArgIt.hasNext()) {
            var varE = varIt.next();
            var argE = argIt.next();
            var oArgE = otherArgIt.next();
            if(varE == null) throw new RuntimeException("This Type parameter Definition needs a variance");
            if(varE == Variance.Invariance && !argE.equals(oArgE)) return false;
            if(varE == Variance.Covariance && !argE.subType(oArgE)) return false;
            if(varE == Variance.Contravariance && !oArgE.subType(argE)) return false;
        }
        return true;
    }

    @Override
    public boolean subType(Instance other){
        if(!isData()) return false;
        var data = base.asData();
        if(other.isData() && other instanceof DefInstance di){
            var otherData = di.base.asData();
            if(!data.subTypeOf(otherData)) return false;
            //This is the invariant generic applies check
            if(arguments == null) throw new RuntimeException("Arguments must be present before subtype checks are possible");
            return argSubType(di);
        } else {
            return false;
        }
    }

    @Override
    public Instance substitute(List<Instance> generics) {
        var argsS = new LinkedList<Instance>();
        for(Instance inst: arguments){
            var res = inst.substitute(generics);
            if(res == null) return null;
            argsS.add(res);
        }
        return new DefInstance(base, argsS);
    }

    @Override
    public void match(Instance match, Instance[] extractedGenerics) {
        if(match instanceof DefInstance di){
            if(di.arguments.size() == arguments.size()){
                int i = 0;
                for(Instance inst:arguments){
                    inst.match(di.arguments.get(i++), extractedGenerics);
                }
            }
        }
    }

    @Override
    public boolean isFunction(){
        return base.getKind() == Definition.Kind.Function;
    }

    @Override
    public boolean isMultiMethod(){
        return base.getKind() == Definition.Kind.MultiMethod;
    }

    @Override
    public boolean isTrait() {
        return base.getKind() == Definition.Kind.Trait;
    }

    @Override
    public boolean isData() {
        return base.getKind() == Definition.Kind.Data;
    }

    @Override
    public boolean isGeneric() { return false; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DefInstance instance = (DefInstance) o;
        return base.equals(instance.base) && Objects.equals(arguments, instance.arguments);
    }

    @Override
    public int hashCode() {
        return Objects.hash(base, arguments);
    }

    public void collectDataHierarchy(LinkedHashSet<Instance> res) {
        var curDef = base.asData();
        while (curDef != null) {
            res.add(new DefInstance(curDef, arguments));
            curDef = curDef.getParent();
        }
    }

    @Override
    public void collectTraitHierarchy(LinkedHashSet<Instance> hierarchy) {
        if(isTrait()) hierarchy.add(this);
        if(isWithTraits()) {
            base.asWithTraits().getTraits().forEach(t -> {
                var inst = t.substitute(arguments);
                inst.collectTraitHierarchy(hierarchy);
            });
        }
    }

    public Definition getBase() {
        return base;
    }

    public List<Instance> getArguments() {
        return arguments;
    }

    @Override
    public String toString() {
        if(arguments == null) return base+"[?]";
        return base+"["+String.join(",",arguments.stream().map(Objects::toString).toList())+"]";
    }
}
