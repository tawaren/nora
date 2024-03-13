package nora.vm.method.lookup;

import com.oracle.truffle.api.CompilerAsserts;
import nora.vm.method.Method;
import nora.vm.types.DispatchCoordinator;

import java.util.Arrays;


public interface Table {

    int getDimensions();

    @FunctionalInterface
    public interface IndexResolver<T> {
        int resolve(T arg, int index);
    }

    <T> Method get(T[] args, IndexResolver<T> f);
    <T> Method get(T[] args, int[] rootInfos);


    void set(int[] index, Method impl);

    static Table create(int[] dimensions) {
        return switch (dimensions.length) {
            case 1 -> new Table1D(dimensions[0]);
            case 2 -> new Table2D(dimensions[0],dimensions[1]);
            case 3 -> new Table3D(dimensions[0], dimensions[1], dimensions[2]);
            default -> null; // needs generic
        };
    }
}

class Table1D implements Table {
    private final Method[] inner;

    Table1D(int d1) {
        this.inner = new Method[d1];
    }

    @Override
    public <T> Method get(T[] args, IndexResolver<T> f) {
        //Note: Arg 1 is the method itself and they may be additional static args
        assert args.length >= 2;
        return inner[f.resolve(args[1],0)];
    }

    @Override
    public <T> Method get(T[] args, int[] rootInfos) {
        assert args.length >= 2;
        CompilerAsserts.partialEvaluationConstant(rootInfos);
        var d1 = DispatchCoordinator.getDispatchIndex(args[1], rootInfos[0]);
        return inner[d1];
    }

    @Override
    public void set(int[] index, Method impl) {
        assert index.length == 1;
        inner[index[0]] = impl;
    }

    @Override
    public int getDimensions() {
        return 1;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(inner);
    }
}

class Table2D implements Table {
    private final Method[] inner;
    private final int d2Mod;

    Table2D(int d1, int d2) {
        this.inner = new Method[d1*d2];
        this.d2Mod = d1;
    }

    private int computeIndex(int d1, int d2){
        return d1+d2*d2Mod;
    }

    @Override
    public <T> Method get(T[] args, IndexResolver<T> f) {
        //Note: Arg 1 is the method itself and they may be additional static args
        assert args.length >= 3;
        return inner[computeIndex(f.resolve(args[1],0),f.resolve(args[2],1))];
    }

    @Override
    public <T> Method get(T[] args, int[] rootInfos) {
        assert args.length >= 3;
        CompilerAsserts.partialEvaluationConstant(rootInfos);
        var d1 = DispatchCoordinator.getDispatchIndex(args[1], rootInfos[0]);
        var d2 = DispatchCoordinator.getDispatchIndex(args[2], rootInfos[1]);
        return inner[computeIndex(d1,d2)];
    }

    @Override
    public void set(int[] index, Method impl) {
        assert index.length == 2;
        inner[computeIndex(index[0],index[1])] = impl;
    }

    @Override
    public int getDimensions() {
        return 2;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(inner);
    }
}

class Table3D implements Table {
    private final Method[] inner;
    private final int d2Mod;
    private final int d3Mod;

    Table3D(int d1, int d2, int d3) {
        this.inner = new Method[d1*d2*d3];
        this.d2Mod = d1;
        this.d3Mod = d1*d2;

    }

    private int computeIndex(int d1, int d2, int d3){
        return d1+d2*d2Mod+d3*d3Mod;
    }

    @Override
    public <T> Method get(T[] args, IndexResolver<T> f) {
        //Note: Arg 1 is the method itself and they may be additional static args
        assert args.length >= 4;
        return inner[computeIndex(f.resolve(args[1],0),f.resolve(args[2],1), f.resolve(args[3],2))];
    }

    @Override
    public <T> Method get(T[] args, int[] rootInfos) {
        assert args.length >= 4;
        CompilerAsserts.partialEvaluationConstant(rootInfos);
        var d1 = DispatchCoordinator.getDispatchIndex(args[1], rootInfos[0]);
        var d2 = DispatchCoordinator.getDispatchIndex(args[2], rootInfos[1]);
        var d3 = DispatchCoordinator.getDispatchIndex(args[3], rootInfos[2]);
        return inner[computeIndex(d1,d2,d3)];
    }

    @Override
    public void set(int[] index, Method impl) {
        assert index.length == 3;
        inner[computeIndex(index[0],index[1],index[2])] = impl;
    }

    @Override
    public int getDimensions() {
        return 3;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(inner);
    }
}
