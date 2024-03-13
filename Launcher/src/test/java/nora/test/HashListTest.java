package nora.test;

import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class HashListTest {

    @Test
    public void listHashCode() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list = interpreter.run("somePkg.MyModule::makeList", 200);
            var res = interpreter.run("somePkg.MyModule::hashCodeList",list);
            assertEquals(res.asInt(), 623100);
            var list2 = interpreter.run("somePkg.MyModule::makeListOther", 200);
            var res2 = interpreter.run("somePkg.MyModule::hashCodeList",list2);
            assertEquals(res2.asInt(), 933069);

        }
    }
}
