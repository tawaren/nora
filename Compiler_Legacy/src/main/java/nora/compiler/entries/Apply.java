package nora.compiler.entries;

import nora.compiler.entries.resolved.Variance;

public record Apply(Variance variance, Instance inst) {}
