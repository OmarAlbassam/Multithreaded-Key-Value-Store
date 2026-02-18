package Benchmarks;

import Logic.DataStore;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class DirectBenchmark {

    private static final int NUM_THREADS = 100;
    private static final int OPS_PER_THREAD = 60000;

    public static void main(String[] args) throws Exception {
        DataStore<String> dataStore = new DataStore<>();
        CountDownLatch latch = new CountDownLatch(NUM_THREADS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        System.out.println("Starting direct benchmark with " + NUM_THREADS + " threads, " + OPS_PER_THREAD + " cycles each...");
        System.out.println("No TCP overhead — calling Logic.DataStore.handler() directly.");

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < NUM_THREADS; i++) {
            int threadId = i;
            Thread.ofVirtual().start(() -> {
                try {
                    for (int j = 0; j < OPS_PER_THREAD; j++) {
                        String key = "t" + threadId + "_k" + j;
                        String value = "v" + j;

                        // SET
                        String setResult = dataStore.handler("SET " + key + " " + value);
                        if ("OK".equals(setResult)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            System.out.println("[FAIL] Thread " + threadId + " SET " + key + " -> " + setResult);
                        }

                        // GET and verify
                        String getResult = dataStore.handler("GET " + key);
                        if (value.equals(getResult)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            System.out.println("[FAIL] Thread " + threadId + " GET " + key + " expected=" + value + " got=" + getResult);
                        }

                        // DEL
                        String delResult = dataStore.handler("DEL " + key);
                        if ("Deleted!".equals(delResult)) {
                            successCount.incrementAndGet();
                        } else {
                            failureCount.incrementAndGet();
                            System.out.println("[FAIL] Thread " + threadId + " DEL " + key + " -> " + delResult);
                        }
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        long elapsed = System.currentTimeMillis() - startTime;
        int totalOps = successCount.get() + failureCount.get();

        System.out.println();
        System.out.println("--- Direct Benchmark Results ---");
        System.out.println("Threads:        " + NUM_THREADS);
        System.out.println("Shards:         " + DataStore.NUM_SHARDS);
        System.out.println("Cycles/thread:  " + OPS_PER_THREAD + " (3 ops each: SET, GET, DEL)");
        System.out.println("Total ops:      " + totalOps);
        System.out.println("Successes:      " + successCount.get());
        System.out.println("Failures:       " + failureCount.get());
        System.out.println("Time:           " + elapsed + "ms");
        if (elapsed > 0) {
            System.out.println("Throughput:     " + (totalOps * 1000L / elapsed) + " ops/sec");
        }
    }
}
