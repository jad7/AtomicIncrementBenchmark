package com.jad.benchmark;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.infra.ThreadParams;
import org.openjdk.jmh.runner.RunnerException;
import sun.misc.Unsafe;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * @author Ilya Krokhmalyov jad7kii@gmail.com
 * @since 10/20/15
 */

@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 1, jvmArgsAppend = {"-Xmx1G", "-Xms1G", "-server", "-XX:+AggressiveOpts",
        "-XX:+UseG1GC",
        "-XX:PermSize=10m",
        "-XX:NewRatio=5",
        "-XX:SurvivorRatio=5",
})
//@Threads(2)
@Measurement(iterations = 3, time = 20, timeUnit = TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
public class IncrementBench {

    private static Unsafe unsafe = UnsafeProvider.getUnsafe();
    private static long offsetIntValue;

    static {
        try {
            offsetIntValue = unsafe.objectFieldOffset
                    (IncrementBench.class.getDeclaredField("intValue"));
        } catch (Exception ex) {
            throw new Error(ex);
        }
    }

    private final Object lock = new Object();

    private AtomicInteger atomicInt = new AtomicInteger();

    private volatile int intValue;

    @Param({"1", "2", "4", "8", "16"})
    private int workSize;

    public static void main(String[] args) throws RunnerException, InterruptedException {
        Util.runOnMultithread(IncrementBench.class.getSimpleName());
    }

    @Setup(Level.Trial)
    public synchronized void init(BenchmarkParams params) {
        intValue = 0;
        atomicInt.set(0);
        if (!params.getBenchmark().endsWith("WithWork") && workSize > 1) {
            System.exit(0);
        }
    }

    @State(Scope.Thread)
    public static class Worker {
        private Random random;

        @Setup
        public void init(ThreadParams threadParams) {
            random = new Random(threadParams.getThreadIndex());
        }

        @CompilerControl(CompilerControl.Mode.INLINE)
        public void work(int workSize) {
            Blackhole.consumeCPU(random.nextInt(workSize));
        }
    }

    @CompilerControl(CompilerControl.Mode.INLINE)
    private int customAtomic() {
        int ret;
        for (;;) {
            ret = intValue;
            if (unsafe.compareAndSwapInt(this, offsetIntValue, ret, ++ret)) {
                break;
            }
            LockSupport.parkNanos(1);
        }
        return ret;
    }


    /////////Benchmarks


    @Benchmark
    public int incrementAtomic() {
        return atomicInt.incrementAndGet();
    }

    @Benchmark
    public int incrementCustomAtomic() {
        return customAtomic();
    }

    @Benchmark
    public int lockInt() {
        synchronized (lock) {
            return ++intValue;
        }
    }

    @Benchmark
    public void onlyWithWork(Worker worker) {
        worker.work(workSize);
    }

    @Benchmark
    public int incrementAtomicWithWork(Worker worker) {
        worker.work(workSize);
        return atomicInt.incrementAndGet();
    }

    @Benchmark
    public int incrementCustomAtomicWithWork(Worker worker) {
        worker.work(workSize);
        return customAtomic();
    }

    @Benchmark
    public int lockIntWithWork(Worker worker) {
        worker.work(workSize);
        synchronized (lock) {
            return ++intValue;
        }
    }


    /*public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(IncrementBench.class.getSimpleName()+ ".*Work")
                .forks(1)
                //.addProfiler(LinuxPerfProfiler.class)
                //.addProfiler(StackProfiler.class)
                .threads(2)
                .build();

        new Runner(opt).run();
    }*/


}
