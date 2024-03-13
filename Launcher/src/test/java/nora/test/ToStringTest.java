package nora.test;

import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ToStringTest {

    @Test
    public void listHashCode() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list = interpreter.run("somePkg.MyModule::makeList", 200);
            var res = interpreter.run("somePkg.MyModule::stringifyList",list);
            var list2 = interpreter.run("somePkg.MyModule::makeList", 100);
            var res2 = interpreter.run("somePkg.MyModule::stringifyList",list2);
            assertNotEquals(res,res2);
            StringBuilder expected = new StringBuilder();
            for(int i = 200; i > 0; i--){
                expected.append("{");
                expected.append(i);
                expected.append(", ");
            }
            expected.append("{}");
            for(int i = 200; i > 0; i--){
                expected.append("}");
            }
            assertEquals(res.asString(), expected.toString());
        }
    }
}
