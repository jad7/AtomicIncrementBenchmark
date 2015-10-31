package com.jad.benchmark;

import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.StackProfiler;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.*;

/**
 * @author Ilya Krokhmalyov YC14IK1
 * @since 10/26/15
 */

public class Util {

    public static void runOnMultithread(Class test) throws RunnerException, InterruptedException {
        runOnMultithread(test.getSimpleName());
    }

    public static void runOnMultithread(String test) throws RunnerException, InterruptedException {
        int cpus = Runtime.getRuntime().availableProcessors();
        Set<String> names= new LinkedHashSet<>();
        Map<String, double[]> map = new HashMap<>();
        //List<Collection<RunResult>> results = new ArrayList<>(cpus +1);
        for (int i = 1; i < cpus + 2; i++) {
            Options opt = new OptionsBuilder()
                    //
                    .addProfiler(StackProfiler.class)
                    .include(test)
                    .threads(i)
                    .build();

            Collection<RunResult> run = new Runner(opt).run();
            for (RunResult runResult : run) {
                String label = runResult.getPrimaryResult().getLabel();
                BenchmarkParams params = runResult.getParams();
                Collection<String> paramsKeys = params.getParamsKeys();
                for (String paramsKey : paramsKeys) {
                    label += ";"+paramsKey + params.getParam(paramsKey);
                }

                double[] doubles = map.get(label);
                if (doubles == null) {
                    doubles = new double[cpus + 1];
                    map.put(label, doubles);
                }
                doubles[i - 1] = runResult.getPrimaryResult().getScore();
            }

            /*if (names.isEmpty()) {
                for (RunResult runResult : run) {
                    names.add(runResult.getPrimaryResult().getLabel());
                }

            }

            results.add(run);*/
        }

        /*System.out.println("\n\n\nThreads,Label,Value");
        for (Collection<RunResult> result : results) {
            for (RunResult runResult : result) {
                System.out.println(runResult.getParams().getThreads() + "," +
                        "\"" + runResult.getPrimaryResult().getLabel() + "\"," +
                        runResult.getPrimaryResult().getScore());
            }
        }*/
        for (int i = 1; i < cpus + 2; i++) {
            System.out.print("," + i);
        }
        System.out.print("\n");
        for (Map.Entry<String, double[]> stringEntry : map.entrySet()) {
            System.out.print("\"" + stringEntry.getKey() + "\"");
            for (double v : stringEntry.getValue()) {
                System.out.print("," + v);
            }
            System.out.println();
        }

        Thread.sleep(100);

    }

    public static void main(String[] args) throws RunnerException, InterruptedException {
        runOnMultithread(args[0]);
    }
}
