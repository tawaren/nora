package nora.compiler.entries.resolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.interfaces.Function;

import java.util.List;

public class ResolvedFunction extends ResolvedAbstractFunction implements Function {
    private final List<Argument> args;

    public ResolvedFunction(String fullyQualifiedName, Function.RecursionMode mode, Instance returnType, Expr body, List<Generic> generics, List<Argument> args) {
        super(fullyQualifiedName, mode, returnType, body, generics);
        this.args = args;
    }

    public List<Argument> getArgs() {
        return args;
    }


}
