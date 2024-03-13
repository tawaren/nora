package nora.compiler.entries.interfaces;

import nora.compiler.entries.Definition;

public interface Function extends Definition, Callable {
    enum RecursionMode {
        NotRecursive,
        CallRecursive,
        ContextRecursive
    }
}
