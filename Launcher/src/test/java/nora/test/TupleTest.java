package nora.test;


import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class TupleTest {
    @Test
    public void packTuple() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var tuple = interpreter.run("somePkg.MyModule::pack", 1,2);
            var tuple2 = interpreter.run("somePkg.MyModule::pack", 3,2);
            var tuple3 = interpreter.run("somePkg.MyModule::pack", 1,3);
            assertNotEquals(tuple,tuple2);
            assertNotEquals(tuple,tuple3);
            assertNotEquals(tuple2,tuple3);
            //Todo: tuple.getMemberKeys() does not work, make work
            assertEquals(tuple.getMember("_0").asLong(), 1);
            assertEquals(tuple.getMember("_1").asInt(), 2);
        }
    }
}
