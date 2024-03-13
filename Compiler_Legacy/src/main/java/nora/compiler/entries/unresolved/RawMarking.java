package nora.compiler.entries.unresolved;
/*
import nora.compiler.entries.ref.Reference;
import nora.compiler.entries.ref.Resolvable;
import nora.compiler.entries.resolved.Marking;
import nora.compiler.resolver.ContextResolver;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

public final class RawMarking implements Resolvable<Marking> {
    //Todo: Parameterized??? -- makes it more complicated to apply
    private final Reference target;
    private final List<Reference> markers = new LinkedList<>();

    public RawMarking(Reference target) {
        this.target = target;
    }

    public void addTrait(Reference marker){
        markers.add(marker);
    }

    @Override
    public Marking resolve(ContextResolver resolver) {
        var nTarget = target.resolve(resolver);
        var nMarkers =  markers.stream().map(m -> m.resolve(resolver).asTrait()).collect(Collectors.toSet());
        return new Marking(nTarget, nMarkers);
    }
}
*/