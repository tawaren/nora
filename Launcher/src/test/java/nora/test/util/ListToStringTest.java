package nora.util;

import nora.launcher.Interpreter;

import java.io.IOException;

public class ListToStringTest {

    public void run() throws IOException {
        try (var interpreter = new Interpreter("src/main/code/build")){
            var makeIt1 = interpreter.run("somePkg.MyModule::makeList", 200);
            var res =  interpreter.bench(1000000, "somePkg.MyModule::stringifyList", makeIt1);
            //var res =  interpreter.bench(5, "myPkg.myModule::fib", (long)47);
            System.out.println(res);
            System.out.println(makeIt1);

        }
    }

    public static void main(String[] args) throws IOException {
        new ListToStringTest().run();
    }
}
