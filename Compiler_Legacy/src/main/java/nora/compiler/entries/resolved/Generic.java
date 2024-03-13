package nora.compiler.entries.resolved;

import nora.compiler.entries.Instance;

public record Generic(String name, Variance variance, Instance bound, Instance hint) {}
