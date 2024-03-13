package nora.launcher;

import org.graalvm.polyglot.Context;
import org.graalvm.polyglot.SandboxPolicy;
import org.graalvm.polyglot.Value;

import java.io.Closeable;
import java.io.IOException;
import java.util.function.Supplier;

import static org.graalvm.polyglot.PolyglotAccess.ALL;

public class Interpreter implements Closeable {
    private final Context context;

    public Interpreter(String srcDir, boolean runSave) {
        context = Context.newBuilder("nora")
                .allowExperimentalOptions(true)
                .allowAllAccess(false)
                .sandbox(SandboxPolicy.TRUSTED)
                //.option("compiler.TracePerformanceWarnings", "all")
                //.option("compiler.TraceInlining", "true")
                //.option("engine.TraceSplitting", "true")
                //.option("engine.CompilationStatistics", "true")
                //.option("compiler.TraceMethodExpansion", "truffleTier")
                //.option("compiler.MethodExpansionStatistics", "truffleTier")
                //.option("compiler.TraceNodeExpansion", "truffleTier")
                //.option("compiler.InstrumentBoundaries", "true") -- This leads to compile errors
                //.option("engine.SpecializationStatistics", "true")
                //.option("engine.TraceTransferToInterpreter", "true")
                //.option("engine.TraceCompilationAST", "true")
                //.option("engine.CompilationFailureAction", "Print")
                //.option("log.file", "truffle.log")
                .environment("base-dir", srcDir)
                .environment("run-save", runSave+"")
                .build();
    }

    public Interpreter(String srcDir) {
        this(srcDir, true);
    }

    public Value run(String id, Object... args){
        Value result = context.parse("nora", id);

        var newArgs = new Object[args.length+1];
        //Should be safe as long as it is not a closure
        newArgs[0] = null;
        //todo see if they are strings nums, ....
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return result.execute(newArgs);
    }

    public Supplier<Value> executable(String id, Object... args){
        Value result = context.parse("nora", id);
        var newArgs = new Object[args.length+1];
        //Should be safe as long as it is not a closure
        newArgs[0] = null;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return () -> result.execute(newArgs);
    }

    public Value bench(int num, String id, Object... args){
       // Value result = context.eval("nora", id+"("+ String.join(",", Arrays.stream(args).map(Objects::toString).toList())+")");

        Value result = context.parse("nora", id);

        var newArgs = new Object[args.length+1];
        //Should be safe as long as it is not a closure
        newArgs[0] = null;
        //todo see if they are strings nums, ....
        System.arraycopy(args, 0, newArgs, 1, args.length);
        Value res = null;
        for(int j = 0; j < 2; j++){
            var t0 = System.nanoTime();
            for(int i = 0; i < num; i++){
                res =  result.execute(newArgs);
            }
            var fin = System.nanoTime()-t0;
            System.out.println("Executed "+num+" times in "+fin+"ns ["+(fin/num)+"ns per iter]");
        }
        return res;
    }

    @Override
    public void close() throws IOException {
        try {
            context.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
