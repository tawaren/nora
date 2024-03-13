package nora.compiler.entries.resolved;

import nora.compiler.entries.DefInstance;
import nora.compiler.entries.Instance;
import nora.compiler.entries.interfaces.Trait;
import nora.compiler.entries.interfaces.WithTraits;

import java.util.List;
import java.util.Set;

public class ResolvedTrait extends ResolvedDefinition implements Trait {
    private final Set<Instance> traits;
    private final List<Generic> generics;

    public ResolvedTrait(String fullyQualifiedName, List<Generic> generics, Set<Instance> traits) {
        super(fullyQualifiedName);
        this.generics = generics;
        this.traits = traits;
    }

    private Boolean valid = null;
    private boolean cacheValidationRes(boolean res){
        if(!res) throw new RuntimeException("Validation failed"); //Better for now
        valid = res;
        return res;
    }


    @Override
    public boolean validateAndInfer() {
        if(valid != null) return valid;
        //Cycle detection
        valid = false;
        for(Instance m: traits){
            if(!m.validateAndInfer(Variance.Covariance)) cacheValidationRes(false);
        }
        return cacheValidationRes(true);
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        if(!validateAndInfer()) return false;
        if(generics.size() != arguments.size()) return false;
        int i = 0;
        for(Instance arg:arguments) {
            var gen = generics.get(i++);
            var bound = gen.bound();
            if(position == null){
                if(!arg.validateAndInfer(null)) return false;
            } else {
                if(!arg.validateAndInfer(position.flipBy(gen.variance()))) return false;
            }
            if(bound != null){
                var genTyp = bound.substitute(arguments);
                if(!arg.fulfills(genTyp)) return false;
            }
        }
        return true;
    }

    @Override
    public boolean hasTrait(Trait trait) {
        if(equals(trait)) return true;
        return traits.stream().anyMatch(t -> {
            if(t.isTrait() && t instanceof DefInstance di){
                return di.getBase().asTrait().hasTrait(trait);
            }
            return false;
        });
    }

    @Override
    public Set<Instance> getTraits() {
        return traits;
    }

    @Override
    public Kind getKind() {
        return Kind.Trait;
    }

    @Override
    public Trait asTrait() {
        return this;
    }

    @Override
    public WithTraits asWithTraits() { return this; }

    //Todo: This is work in progress and neither by parser nor by validator supported yet
    //Just to program against for now
    @Override
    public List<Generic> getGenerics(){
        return generics;
    }

    public int numGenerics() {
        return generics.size();
    }

}
