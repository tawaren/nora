package nora.compiler.entries.interfaces;

import nora.compiler.entries.Instance;


public interface Callable extends WithArguments{
    Instance getReturnType();
}
