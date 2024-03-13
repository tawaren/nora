package nora.test;


import nora.launcher.Interpreter;
import nora.test.util.Config;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;


public class PrimitiveHierarchyTest {
    @Test
    public void primitiveHierarchyTrue() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var b = interpreter.run("somePkg.MyModule::primHierarchies", true);
            assertTrue(b.asBoolean());
        }
    }

    @Test
    public void primitiveHierarchyFalse() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var b = interpreter.run("somePkg.MyModule::primHierarchies", false);
            assertFalse(b.asBoolean());
        }
    }

    @Test
    public void primitiveHierarchyTrueHash() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var b = interpreter.run("somePkg.MyModule::primHierarchyHash", true);
            assertEquals(b.asInt(), 1231);
        }
    }

    @Test
    public void primitiveHierarchyFalseHash() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var b = interpreter.run("somePkg.MyModule::primHierarchyHash", false);
            assertEquals(b.asInt(), 0);
        }
    }


    @Test
    public void primitiveHierarchyNoCache() throws IOException {
        try(Interpreter interpreter =  new Interpreter(Config.buildFolder)) {
            var a = interpreter.run("somePkg.MyModule::primHierarchies", true);
            var b = interpreter.run("somePkg.MyModule::primHierarchies", false);
            var c = interpreter.run("somePkg.MyModule::primHierarchyHash", true);
            var d = interpreter.run("somePkg.MyModule::primHierarchyHash", false);
            assertNotEquals(a,b);
            assertNotEquals(c,d);
        }
    }
}
