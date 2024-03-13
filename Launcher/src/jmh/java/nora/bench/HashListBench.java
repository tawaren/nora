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
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class HashListBench {
    private Interpreter interpreter;
    private Supplier<Value> function;
    private ConsList<Integer> javaList;

    @Setup
    public void setup(){
        interpreter = new Interpreter(Config.buildFolder, false);
        var list = interpreter.run("somePkg.MyModule::makeList", 200);
        function = interpreter.executable("somePkg.MyModule::hashCodeList",list);
        javaList = ConsList.build((aggr, elem) -> new Cons<>(elem,aggr), new Nil<>(),200);
    }

    @Benchmark
    public void nora(Blackhole bh) {
        bh.consume(function.get());
    }

    @Benchmark
    public void java(Blackhole bh) {
        bh.consume(javaList.hashCode());
    }

    @TearDown
    public void close() throws IOException {
       interpreter.close();
    }

}
