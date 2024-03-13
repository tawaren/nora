package nora.test;

import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BaselineTest {
    @Test
    public void helloWorld() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::helloWorld");
            assertEquals(res.asInt(), 0);
        }
    }
}
