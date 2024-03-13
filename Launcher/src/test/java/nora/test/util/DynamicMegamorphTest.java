package nora.util;

import nora.launcher.Interpreter;

import java.io.IOException;

public class DynamicMegamorphTest {

    public void run() throws IOException {
        var interpreter = new Interpreter("src/main/code");
        var res =  interpreter.bench(100000000, "myPkg.myModule::subAddMulDiv");
        //var res =  interpreter.bench(5, "myPkg.myModule::fib", (long)47);
        System.out.println(res);
    }

    public static void main(String[] args) throws IOException {
        new DynamicMegamorphTest().run();
    }
}
