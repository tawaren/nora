package nora.test;

import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.*;

public class FibonacciTest {
    @Test
    public void executeFastFib() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::fastFib",15);
            assertEquals(610, res.asInt());
            var res2 = interpreter.run("somePkg.MyModule::fastFib",5);
            assertNotEquals(res,res2);
        }
    }

    @Test
    public void executeSlowFibSmall() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::fib",25);
            assertEquals(res.asLong(), 75025);
            var res2 = interpreter.run("somePkg.MyModule::fib",5);
            assertNotEquals(res,res2);
        }
    }

    @Test
    public void executeFastFibBig() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::fastFibNum",200);
            assertEquals(res.asBigInteger(), new BigInteger("280571172992510140037611932413038677189525"));
            var res2 = interpreter.run("somePkg.MyModule::fastFibNum",5);
            assertNotEquals(res,res2);
        }
    }

}
