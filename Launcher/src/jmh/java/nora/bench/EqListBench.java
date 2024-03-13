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
public class EqListBench {
    private Interpreter interpreter;
    private Supplier<Value> function;
    private ConsList<Integer> javaList1;
    private ConsList<Integer> javaList2;

    @Setup
    public void setup(){
        interpreter = new Interpreter(Config.buildFolder, false);
        var list1 = interpreter.run("somePkg.MyModule::makeList", 200);
        var list2 = interpreter.run("somePkg.MyModule::makeList", 200);
        function = interpreter.executable("somePkg.MyModule::compareList",list1,list2);
        javaList1 = ConsList.build((aggr, elem) -> new Cons<>(elem,aggr), new Nil<>(),200);
        javaList2 = ConsList.build((aggr, elem) -> new Cons<>(elem,aggr), new Nil<>(),200);

    }

    @Benchmark
    public void nora(Blackhole bh) {
        bh.consume(function.get());
    }

    @Benchmark
    public void java(Blackhole bh) {
        bh.consume(javaList1.equals(javaList2));
    }

    @Benchmark
    public void java_opt(Blackhole bh) {
        bh.consume(ConsList.eq_opt(javaList1, javaList2));
    }


    @TearDown
    public void close() throws IOException {
       interpreter.close();
    }

}
