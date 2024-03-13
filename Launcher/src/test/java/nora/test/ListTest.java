package nora.test;

import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ListTest {

    @Test
    public void makeList() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::makeList", 200);
            var res2 = interpreter.run("somePkg.MyModule::makeList", 100);
            assertNotEquals(res, res2);
            //Use reflection to check the result
            //  We are no longer in Nora so this is the only way
            for(int i = 200; i > 0; i = i-1){
                assertEquals(res.getMetaObject().getMetaSimpleName(), "Cons");
                var head = res.getMember("head");
                assertEquals(head.asInt(),i);
                res = res.getMember("tail");
            }
            assertEquals(res.getMetaObject().getMetaSimpleName(), "Nil");
        }
    }

    @Test
    public void mapFilter() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var list = interpreter.run("somePkg.MyModule::makeList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeList", 100);
            var res = interpreter.run("somePkg.MyModule::mapFilterList",list);
            var res2 = interpreter.run("somePkg.MyModule::mapFilterList",list2);
            assertNotEquals(res, res2);
            //Use reflection to check the result
            //  We are no longer in Nora so this is the only way
            for(int i = 400; i > 0; i = i-4){
                assertEquals(res.getMetaObject().getMetaSimpleName(), "Cons");
                var head = res.getMember("head");
                assertEquals(head.asInt(),i);
                res = res.getMember("tail");
            }
            assertEquals(res.getMetaObject().getMetaSimpleName(), "Nil");
        }
    }


    @Test
    public void makeMapFilter() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::makeAndMapList",100);
            var res2 = interpreter.run("somePkg.MyModule::makeAndMapList",100);
            //we run twice to ensure the constant optimizer does not screw up
            assertEquals(res.toString(),res2.toString());
            //Use reflection to check the result
            //  We are no longer in Nora so this is the only way
            for(int i = 200; i > 0; i = i-4){
                assertEquals(res.getMetaObject().getMetaSimpleName(), "Cons");
                var head = res.getMember("head");
                assertEquals(head.asInt(),i);
                res = res.getMember("tail");
            }
            assertEquals(res.getMetaObject().getMetaSimpleName(), "Nil");
        }
    }

    @Test
    public void makeMapFilterFast() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var res = interpreter.run("somePkg.MyModule::makeAndMapListFast", 100);
            var res2 = interpreter.run("somePkg.MyModule::makeAndMapListFast", 100);
            //we run twice to ensure the constant optimizer does not screw up
            assertEquals(res.toString(),res2.toString());
            //Use reflection to check the result
            //  We are no longer in Nora so this is the only way
            for(int i = 200; i > 0; i = i-4){
                assertEquals(res.getMetaObject().getMetaSimpleName(), "Cons");
                var head = res.getMember("head");
                assertEquals(head.asInt(),i);
                res = res.getMember("tail");
            }
            assertEquals(res.getMetaObject().getMetaSimpleName(), "Nil");
        }
    }





}
