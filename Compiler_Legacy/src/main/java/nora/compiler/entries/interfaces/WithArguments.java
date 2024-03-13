package nora.compiler.entries.interfaces;

import nora.compiler.entries.resolved.Argument;

import java.util.List;

public interface WithArguments extends Parametric{
    List<Argument> getArgs();
}
