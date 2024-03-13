package nora.compiler.entries.interfaces;

import nora.compiler.entries.Instance;
import nora.compiler.entries.resolved.Generic;
import nora.compiler.entries.resolved.Variance;

import java.util.List;

public interface Parametric {
    boolean validateApplication(List<Instance> arguments, Variance position);
    List<Generic> getGenerics();
    int numGenerics();
}
