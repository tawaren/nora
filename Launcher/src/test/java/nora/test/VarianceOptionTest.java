package nora.test;


import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class VarianceOptionTest {
    @Test
    public void haveSomeVariance() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var tuple = interpreter.run("somePkg.Collection::varianceTestMain", 2);
            assertEquals(tuple.getMember("e").asInt(), 1);
        }
    }
}
