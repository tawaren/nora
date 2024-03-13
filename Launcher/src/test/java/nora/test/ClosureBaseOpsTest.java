package nora.test;


import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotEquals;


public class ClosureBaseOpsTest {
    @Test
    public void equalClosureLists() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list1 = interpreter.run("somePkg.MyModule::makeClosureList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeSameClosureList", 200);
            var list3 = interpreter.run("somePkg.MyModule::makeOtherClosureList", 200);
            var res = interpreter.run("somePkg.MyModule::compareFunList",list1,list2);
            assertTrue(res.asBoolean());
            assertNotEquals(list1,list3);
        }
    }

    @Test
    public void hashClosureLists() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list1 = interpreter.run("somePkg.MyModule::makeClosureList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeSameClosureList", 200);
            var list3 = interpreter.run("somePkg.MyModule::makeOtherClosureList", 200);
            var hash1 = interpreter.run("somePkg.MyModule::hashFunList",list1);
            var hash2 = interpreter.run("somePkg.MyModule::hashFunList",list2);
            var hash3 = interpreter.run("somePkg.MyModule::hashFunList",list3);
            assertEquals(hash1.asInt(), hash2.asInt());
            assertNotEquals(hash1.asInt(),hash3.asInt());
        }
    }

    @Test
    public void toStringClosureLists() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list1 = interpreter.run("somePkg.MyModule::makeClosureList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeSameClosureList", 200);
            var list3 = interpreter.run("somePkg.MyModule::makeOtherClosureList", 200);
            var string1 = interpreter.run("somePkg.MyModule::stringifyFunList",list1);
            var string2 = interpreter.run("somePkg.MyModule::stringifyFunList",list2);
            var string3 = interpreter.run("somePkg.MyModule::stringifyFunList",list3);
            assertEquals(string1.asString(), string2.asString());
            assertNotEquals(string1.asString(),string3.asString());

            var stringIdS = string1.asString().indexOf("<");
            var stringIdE = string1.asString().indexOf(">");
            var stringId = string1.asString().substring(stringIdS,stringIdE);
            assertTrue(stringId.startsWith("<Closure@nora.vm.runtime.data.IdentityObject@"));

            StringBuilder expected = new StringBuilder();
            for(int i = 200; i > 0; i--){
                expected.append("{");
                expected.append(stringId);
                expected.append(">, ");
            }
            expected.append("{}");
            for(int i = 200; i > 0; i--){
                expected.append("}");
            }

            assertEquals(string1.asString(), expected.toString());


        }
    }
}
