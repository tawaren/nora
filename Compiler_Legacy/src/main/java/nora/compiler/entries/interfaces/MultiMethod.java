package nora.compiler.entries.interfaces;

import nora.compiler.entries.Definition;
import nora.compiler.entries.resolved.Argument;
import nora.compiler.entries.resolved.ResolvedCaseMethod;

import java.util.List;

public interface MultiMethod extends Definition, Callable {
    void addCaseMethod(ResolvedCaseMethod cm);

    boolean isSealed();

    boolean isPartial();

    List<Argument> getDynamicArgs();

    List<Argument> getStaticArgs();
}
