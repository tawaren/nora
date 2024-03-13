package nora.compiler.entries.exprs;

import nora.compiler.entries.ref.ParametricReference;
import nora.compiler.resolver.ContextResolver;

import java.math.BigInteger;

public class TypeHintExpr extends UnresolvedExpr {
    private final Expr val;
    private final ParametricReference typ;

    public TypeHintExpr(Expr val, ParametricReference typ) {
        this.val = val;
        this.typ = typ;
    }

    @Override
    public Expr resolve(ContextResolver resolver) {
        return new nora.compiler.entries.exprs.resolved.TypeHintExpr(val.resolve(resolver), typ.resolve(resolver));
    }
}
