package nora.compiler.entries.resolved;

import nora.compiler.entries.Instance;
import nora.compiler.entries.exprs.Expr;
import nora.compiler.entries.interfaces.Callable;
import nora.compiler.entries.interfaces.Function;
import nora.compiler.entries.interfaces.Parametric;
import nora.compiler.entries.interfaces.WithArguments;
import nora.compiler.processing.TypeCheckContext;
import nora.compiler.processing.TypeCollector;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;

public abstract class ResolvedAbstractFunction extends ResolvedDefinition implements Function {
    private final Function.RecursionMode mode;
    private Instance returnType;
    private final Expr body;
    private final List<Generic> generics;

    public ResolvedAbstractFunction(String fullyQualifiedName, Function.RecursionMode mode, Instance returnType, Expr body, List<Generic> generics) {
        super(fullyQualifiedName);
        this.mode = mode;
        this.returnType = returnType;
        this.body = body;
        this.generics = generics;
    }

    protected Boolean valid = null;
    protected boolean cacheValidationRes(boolean res){
        if(!res) throw new RuntimeException("Validation failed"); //Better for now
        valid = res;
        return res;
    }

    @Override
    public boolean validateAndInfer() {
        if(valid != null) {
            if(returnType == null) throw new RuntimeException("Recursive functions can not infere its return type");
            return valid;
        }
        valid = true;
        if(returnType != null && !returnType.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        for(Generic g:generics) {
            var bound = g.bound();
            //Todo: this only holds as long as bound means subType
            //      T <: Something[T]
            //      if(T >: Something[T]) then Contravariance is needed
            if(bound != null && !g.bound().validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        }
        var args = getArgs();
        for(Argument arg:args) if(!arg.typ().validateAndInfer(Variance.Contravariance)) return cacheValidationRes(false);

        TypeCheckContext ctx = TypeCheckContext.get();
        Instance retT;
        try {
            retT = ctx.withSignature(generics, args, c -> {
                if(returnType != null) {
                    return c.withReturnHint(returnType, body::getAndInferenceType);
                } else {
                    var extra = extraReturnHint();
                    if(extra != null){
                        return c.withReturnHint(extra, body::getAndInferenceType);
                    } else {
                        return body.getAndInferenceType(c);
                    }
                }
            });
        } catch (Exception e){
            throw new RuntimeException(fullyQualifiedName,e);
        }

        if(returnType == null) {
            returnType = retT;
        } else {
            if(!retT.subType(returnType)) return cacheValidationRes(false);
        }
        return cacheValidationRes(true);
    }

    @Override
    public boolean validateApplication(List<Instance> arguments, Variance position) {
        if(!validateAndInfer()) return false;
        if(generics.size() != arguments.size()) {
            //Are we a special unrestricted VarArg Def
            // Like tuples & function
            return generics.size() == 0 && TypeCheckContext.get().isSpecialVarargDef(this);
        }
        int i = 0;
        for(Instance arg:arguments) {
            var gen = generics.get(i++);
            if(gen.variance() != null) throw new RuntimeException("Function generics are not supposed to have a variance");
            var bound = gen.bound();
            if(!arg.validateAndInfer(null)) return false;
            if(bound == null) continue;
            var genTyp = bound.substitute(arguments);
            if(!arg.fulfills(genTyp)) return false;
        }

        if(!returnType.validateAndInfer(Variance.Covariance)) return cacheValidationRes(false);
        return true;
    }

    @Override
    public Kind getKind() {
        return Kind.Function;
    }

    @Override
    public Function asFunction() {
        return this;
    }

    @Override
    public Callable asCallable() {
        return this;
    }

    @Override
    public Parametric asParametric() {
        return this;
    }

    @Override
    public WithArguments asWithArguments() {
        return this;
    }

    public Instance getReturnType() {
        return returnType;
    }

    public Expr getBody() {
        return body;
    }

    public List<Generic> getGenerics() {
        return generics;
    }

    public int numGenerics() {
        return generics.size();
    }

    protected Instance extraReturnHint() {return null;}

    @Override
    public void generateCode(Path buildRoot) {
        var path = targetFile(buildRoot);
        try {
            Files.createDirectories(path.getParent());
            try (OutputStreamWriter out = new OutputStreamWriter(Files.newOutputStream(path, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
                out.append("<locals>: ").append(String.valueOf(body.countLocals())).append("\n");
                var args = getArgs();
                switch (mode){
                    case NotRecursive: break;
                    case CallRecursive:
                        out.append("<tail-recursive>: ").append(Integer.toString(args.size())).append("\n");
                        break;
                    case ContextRecursive:
                        out.append("<tail-ctx-recursive>: ").append(Integer.toString(args.size())).append("\n");
                        break;
                }
                out.append("<sig>: (");
                for (Iterator<Argument> it =  args.iterator(); it.hasNext(); ) {
                    var argT = TypeCollector.get().resolveTypeString(it.next().typ(), true);
                    out.append(argT);
                    if (it.hasNext()) out.append(", ");
                }
                out.append(") -> ");
                var retT = TypeCollector.get().resolveTypeString(returnType, true);
                out.append(retT).append("\n");
                out.append("<body>: ");
                body.generateCode(out, 1);
            }
        }catch (IOException e) {
            e.printStackTrace();
        }
    }
}
