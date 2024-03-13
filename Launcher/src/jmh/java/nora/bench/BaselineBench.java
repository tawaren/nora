package nora.bench;

import nora.launcher.Interpreter;
import nora.test.util.Config;
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
public class BaselineBench {
    private Interpreter interpreter;
    private Supplier<Value> function;

    @Setup
    public void setup(){
        interpreter = new Interpreter(Config.buildFolder, false);
        function = interpreter.executable("somePkg.MyModule::helloWorld");
    }

    @Benchmark
    public void nora(Blackhole bh) {
        bh.consume(function.get());
    }

    private int helloWorld() {
        return 0;
    }

    @Benchmark
    public void java(Blackhole bh) {
        bh.consume(helloWorld());
    }


    @TearDown
    public void close() throws IOException {
       interpreter.close();
    }

}
