package nora.compiler.entries.resolved;
/*
import nora.compiler.entries.Command;
import nora.compiler.entries.Definition;
import nora.compiler.entries.interfaces.Trait;

import java.util.Set;

public final class Marking extends Command {
    //Todo: Parameterized??? -- makes it more complicated to apply
    private final Definition target;
    private final Set<Trait> traits;

    public Marking(Definition target, Set<Trait> traits) {
        this.target = target;
        this.traits = traits;
    }

    //Applies the marking -- currently only allows application to templates
    // Later we can have a global Instance -> Marker mapping to allow it to apply to types as well
    @Override
    public void execute() {
        var targ = target.asData();
        if(targ != null) {
            targ.addExtraMarkers(traits);
        } else {
            throw new RuntimeException("Can only add markers to data");
        }
    }
}
*/