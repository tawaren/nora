package nora.bench;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
/*
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 1)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class IntArrBench {
    private int[] int1;
    private int[] int1_2;
    private int[] int4;
    private int[] int4_2;
    private int[] int8;
    private int[] int8_2;
    private int[] int16;
    private int[] int16_2;
    private int[] int32;
    private int[] int32_2;

    @Setup
    public void setup(){
        int1 = new int[1];
        int4 = new int[4];
        int8 = new int[8];
        int16 = new int[16];
        int32 = new int[32];

        for(int i = 0; i < 32; i++){
            if(i < int1.length) int1[i] = i;
            if(i < int4.length) int4[i] = i;
            if(i < int8.length) int8[i] = i;
            if(i < int16.length) int16[i] = i;
            if(i < int32.length) int32[i] = i;
        }

        int1_2 = int1.clone();
        int4_2 = int4.clone();
        int8_2 = int8.clone();
        int16_2 = int16.clone();
        int32_2 = int32.clone();

    }

    @Benchmark
    public void loop_1(Blackhole bh) {
        for(int i = 0; i < int1.length; i++){
            if(int1[i] != int1_2[i]){
                bh.consume(false);
                return;
            }
        }
        bh.consume(true);
    }

    @Benchmark
    public void loop_4(Blackhole bh) {
        for(int i = 0; i < int4.length; i++){
            if(int4[i] != int4_2[i]){
                bh.consume(false);
                return;
            }
        }
        bh.consume(true);
    }

    @Benchmark
    public void loop_8(Blackhole bh) {
        for(int i = 0; i < int8.length; i++){
            if(int8[i] != int8_2[i]){
                bh.consume(false);
                return;
            }
        }
        bh.consume(true);
    }

    @Benchmark
    public void loop_16(Blackhole bh) {
        for(int i = 0; i < int16.length; i++){
            if(int16[i] != int16_2[i]){
                bh.consume(false);
                return;
            }
        }
        bh.consume(true);
    }

    @Benchmark
    public void loop_32(Blackhole bh) {
        for(int i = 0; i < int32.length; i++){
            if(int32[i] != int32_2[i]){
                bh.consume(false);
                return;
            }
        }
        bh.consume(true);
    }

    @Benchmark
    public void equals_1(Blackhole bh) {
        bh.consume(Arrays.equals(int1,int1_2));
    }

    @Benchmark
    public void equals_4(Blackhole bh) {
        bh.consume(Arrays.equals(int4,int4_2));
    }

    @Benchmark
    public void equals_8(Blackhole bh) {
        bh.consume(Arrays.equals(int8,int8_2));
    }

    @Benchmark
    public void equals_16(Blackhole bh) {
        bh.consume(Arrays.equals(int16,int16_2));
    }

    @Benchmark
    public void equals_32(Blackhole bh) {
        bh.consume(Arrays.equals(int32,int32_2));
    }

}
*/