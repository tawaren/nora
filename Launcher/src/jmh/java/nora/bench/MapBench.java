package nora.bench;

import nora.launcher.Interpreter;
import nora.test.util.Config;
import nora.test.util.consListTrmc.ConsListTrmc;
import nora.test.util.consListTrmc.ConsTrmc;
import nora.test.util.consListTrmc.NilTrmc;
import nora.util.consList.Cons;
import nora.util.consList.ConsList;
import nora.util.consList.Nil;
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
public class MapBench {
    private Interpreter interpreter;
    private Supplier<Value> function_rev;
    private Supplier<Value> function;
    private Supplier<Value> function_trmc;
    private ConsList<Integer> javaList;
    private ConsListTrmc<Integer> javaListTrmc;

    @Setup
    public void setup(){
        interpreter = new Interpreter(Config.buildFolder, false);
        var list1 = interpreter.run("somePkg.MyModule::makeList", 200);
        function_rev = interpreter.executable("somePkg.MyModule::mapReverseList", list1);
        function_trmc = interpreter.executable("somePkg.MyModule::mapCtxList", list1);
        function = interpreter.executable("somePkg.MyModule::mapList", list1);
        javaList = ConsList.build((aggr, elem) -> new Cons<>(elem,aggr), new Nil<>(),200);
        javaListTrmc = ConsListTrmc.build((aggr, elem) -> new ConsTrmc<>(elem,aggr), new NilTrmc<>(),200);
    }

    @Benchmark
    public void nora_opt(Blackhole bh) {
        bh.consume(function_trmc.get());
    }
    @Benchmark
    public void nora_rev(Blackhole bh) {
        bh.consume(function_rev.get());
    }

    @Benchmark
    public void nora(Blackhole bh) {
        bh.consume(function.get());
    }

    @Benchmark
    public void java(Blackhole bh) {
        bh.consume(javaList.map(x -> x*2));
    }

    @Benchmark
    public void java_opt(Blackhole bh) {
        bh.consume(javaListTrmc.map(x -> x*2));
    }

    @TearDown
    public void close() throws IOException {
        interpreter.close();
    }

}
