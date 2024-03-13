package nora.test;


import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;


import java.io.IOException;



public class EqListTest {
    @Test
    public void equalLists() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list1 = interpreter.run("somePkg.MyModule::makeList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeList", 200);
            var list3 = interpreter.run("somePkg.MyModule::makeList", 100);
            var res = interpreter.run("somePkg.MyModule::compareList",list1,list2);
            assertTrue(res.asBoolean());
            assertNotEquals(list1,list3);
        }
    }


    @Test
    public void sameSizeDiffElemsLists() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list1 = interpreter.run("somePkg.MyModule::makeList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeListOther", 200);
            var list3 = interpreter.run("somePkg.MyModule::makeListOther", 100);
            var res = interpreter.run("somePkg.MyModule::compareList",list1,list2);
            assertFalse(res.asBoolean());
            assertNotEquals(list2,list3);
        }
    }

    @Test
    public void diffSizeLists() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list1 = interpreter.run("somePkg.MyModule::makeList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeListPlusOne", 200);
            var list3 = interpreter.run("somePkg.MyModule::makeListPlusOne", 200);
            var res = interpreter.run("somePkg.MyModule::compareList",list1,list2);
            assertFalse(res.asBoolean());
            assertNotEquals(list2,list3);
        }
    }
}
