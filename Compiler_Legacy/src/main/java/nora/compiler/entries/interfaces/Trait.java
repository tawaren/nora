package nora.compiler.entries.interfaces;

import nora.compiler.entries.Definition;
import nora.compiler.entries.resolved.Generic;

import java.util.List;

public interface Trait extends Definition, Parametric, WithTraits {
    //Todo: This is work in progress and neither by parser nor by validator supported yet
    List<Generic> getGenerics();
}
