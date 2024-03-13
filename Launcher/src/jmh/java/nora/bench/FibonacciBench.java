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
//@Fork(value = 1, jvmArgsAppend = {"-Xint"})
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class FibonacciBench {
    private Interpreter interpreter;
    private Supplier<Value> function;

    @Setup
    public void setup(){
        interpreter = new Interpreter(Config.buildFolder, false);
        function = interpreter.executable("somePkg.MyModule::fastFib",600);
    }

    @Benchmark
    public void nora(Blackhole bh) {
        bh.consume(function.get());
    }

    public static int innerLoopFib(int n, int val, int prev){
        while (n != 0){
            n = n-1;
            val = val+prev;
            prev = val;
        }
        return prev;
    }

    public static int tailFib(int n, int val, int prev){
        if(n == 0) return prev;
        return tailFib(n -1, val+prev, val);
    }

    public static int fastFib(int n){
        return tailFib(n+1,0,1);
    }

    public static int loopFib(int n){
        return innerLoopFib(n+1,0,1);
    }

    @Benchmark
    public void java(Blackhole bh) {
        bh.consume(fastFib(600));
    }

    @Benchmark
    public void java_opt(Blackhole bh) {
        bh.consume(loopFib(600));
    }

    @TearDown
    public void close() throws IOException {
       interpreter.close();
    }

}
