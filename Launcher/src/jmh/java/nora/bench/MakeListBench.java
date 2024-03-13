package nora.bench;

import nora.test.util.Config;
import nora.util.consList.Cons;
import nora.util.consList.ConsList;
import nora.util.consList.Nil;
import nora.launcher.Interpreter;
import org.graalvm.polyglot.Value;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class MakeListBench {
    private Interpreter interpreter;
    private Supplier<Value> function;
    private Supplier<Value> function_opt;

    @State(Scope.Thread)
    public static class MyState {
        public int length = 100;
    }

    @Setup
    public void setup(){
        interpreter = new Interpreter(Config.buildFolder, false);
        function = interpreter.executable("somePkg.MyModule::makeList", 100);
        function_opt = interpreter.executable("somePkg.MyModule::makeListFast", 100);

    }

    //Todo: Note: Tail Call slows this down considerably
    //            The tail call is in filter and seems not to be unrolled into a loop
    //       Alt would be to do it in compiler
    @Benchmark
    public void nora(Blackhole bh) {
        bh.consume(function.get());
    }

    @Benchmark
    public void nora_opt(Blackhole bh) {
        bh.consume(function_opt.get());
    }

    @Benchmark
    public void java(MyState state, Blackhole bh) {
        bh.consume(ConsList.build((aggr, elem) -> new Cons<>(elem,aggr), new Nil<>(),state.length));
    }

    @Benchmark
    public void java_arrayList(MyState state, Blackhole bh) {
        var arr = new ArrayList<Integer>(state.length);
        for(int i = 0; i < state.length; i++){
            arr.add(i);
        }
        bh.consume(arr);
    }

    @TearDown
    public void close() throws IOException {
        interpreter.close();
    }

}
