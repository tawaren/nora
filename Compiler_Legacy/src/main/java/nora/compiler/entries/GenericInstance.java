package nora.compiler.entries;

import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.resolved.Variance;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class GenericInstance implements Instance {
    public final Variance varianceDeclaration;
    //Todo: do we need a integer instead?
    public final int index;
    //Todo: shall we allow multiple generic bounds
    final Instance bound;

    public GenericInstance(Variance varianceDeclaration, int index, Instance bound) {
        this.varianceDeclaration = varianceDeclaration;
        this.index = index;
        this.bound = bound;
    }


    private Boolean valid = null;
    private boolean cacheValidationRes(boolean res){
        if(!res) throw new RuntimeException("Validation failed"); //Better for now
        valid = res;
        return res;
    }

    @Override
    public boolean validateAndInfer(Variance position) {
        if(valid != null) return valid;
        valid = true;
        if(varianceDeclaration != null && position != null
                && !varianceDeclaration.canBeAppliedTo(position)){
            return cacheValidationRes(false);
        }
        if(bound == null) return cacheValidationRes(true);
        //Bounds are technically evaluated in the argument list
        //  Thus resets variance (it should not even be here)
        //Todo: this only holds as long as bound means subType
        //      T <: Something[T]
        //      if(T >: Something[T]) then Contravariance is needed
        return cacheValidationRes(bound.validateAndInfer(Variance.Covariance));
    }

    @Override
    public boolean fulfills(Instance other){
        if(subType(other)) return true;
        return hasTrait(other);
    }

    @Override
    public Instance findSuperWithTrait(Trait trait) {
        if(bound instanceof DefInstance di) {
            if(di.isTrait() && di.getBase().asWithTraits().hasTrait(trait)) return this;
            if(di.isData()) {
                var parent = di.getBase().asData().getParent();
                if(parent != null) {
                    return new DefInstance(parent, di.getArguments()).findSuperWithTrait(trait);
                }
            }
            return bound.findSuperWithTrait(trait);
        }
        return null;
    }

    @Override
    public boolean hasTrait(Instance trait) {
        if(!trait.isTrait()) return false;
        if(bound == null) return false;
        return bound.hasTrait(trait);
    }

    @Override
    public boolean subType(Instance other){
        if(equals(other)) return true;
        if(bound == null) return false;
        return bound.subType(other);
    }

    @Override
    public Instance substitute(List<Instance> generics) {
        return generics.get(index);
    }

    @Override
    public void match(Instance match, Instance[] extractedGenerics) {
        if(extractedGenerics[index] == null && match != null){
            if(bound != null){
                extractedGenerics[index] = match;
                Optional<Instance> boundTarget = Optional.empty();
                if(bound.isTrait() && bound instanceof DefInstance bi){
                    boundTarget = Instance.collectTraitHierarchy(match).stream().filter(i -> {
                        if(i instanceof DefInstance di) return di.getBase().equals(bi.getBase());
                        return false;
                    }).findAny();
                } else if(bound.isData() && bound instanceof DefInstance di){
                    boundTarget = Instance.collectDataHierarchy(match).stream().filter(i -> {
                        if(i instanceof DefInstance pi) return pi.getBase().equals(di.getBase());
                        return false;
                    }).findAny();
                }
                boundTarget.ifPresent(instance -> bound.match(instance, extractedGenerics));
            } else {
                extractedGenerics[index] = match;
            }
        }
    }

    @Override
    public void collectDataHierarchy(LinkedHashSet<Instance> hierarchy) {
        hierarchy.add(this);
        if(bound == null) return;
        bound.collectDataHierarchy(hierarchy);
    }

    //Todo: does this even make sense on generic or simply return empty?
    @Override
    public void collectTraitHierarchy(LinkedHashSet<Instance> hierarchy) {
        if(bound == null) return;
        if(bound.isTrait()) hierarchy.add(bound);
        if(bound.isWithTraits() && bound instanceof DefInstance di) {
            di.getBase().asWithTraits().getTraits().forEach(t -> {
                hierarchy.add(t);
                t.collectTraitHierarchy(hierarchy);
            });
        }
    }

    @Override
    public boolean isFunction(){
        return false;
    }

    @Override
    public boolean isMultiMethod(){
        return false;
    }

    @Override
    public boolean isTrait() {
        return false;
    }

    @Override
    public boolean isData() {
        return false;
    }

    @Override
    public boolean isGeneric() { return true; }

    public Instance getBound() {
        return bound;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericInstance that = (GenericInstance) o;
        return index == that.index && Objects.equals(bound, that.bound);
    }

    @Override
    public int hashCode() {
        return Objects.hash(index, bound);
    }

    @Override
    public String toString() {
        if(bound == null) return  "$"+index;
        return "$"+index+":"+bound;
    }
}
