package nora.launcher;

import java.io.IOException;

public class Main {

    public static void main(String... args) throws IOException {
        try(Interpreter interpreter =  new Interpreter("Compiler/src/main/code/build")) {
            var list1 = interpreter.run("somePkg.MyModule::makeList", 200);
            var list2 = interpreter.run("somePkg.MyModule::makeList", 200);
            var res = interpreter.run("somePkg.MyModule::compareList",list1,list2);
        }
    }
}
