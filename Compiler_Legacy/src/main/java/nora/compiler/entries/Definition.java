package nora.compiler.entries;


import nora.compiler.entries.interfaces.*;
import nora.compiler.entries.interfaces.Module;
import nora.compiler.entries.ref.Resolvable;

import java.nio.file.Path;

public interface Definition extends Resolvable<Definition> {
    boolean validateAndInfer();

    void generateCode(Path buildRoot);

    enum Kind {
        Root,
        Package,
        Module,
        Data,
        Function,
        Trait,
        MultiMethod,
    }

    Root ROOT = new Root("");

    boolean isResolved();

    Kind getKind();
    Module asModule();
    Data asData();
    MultiMethod asMultiMethod();
    Trait asTrait();
    Function asFunction();
    Callable asCallable();
    Parametric asParametric();
    WithArguments asWithArguments();

    WithTraits asWithTraits();

    String getFullyQualifiedName();

}
