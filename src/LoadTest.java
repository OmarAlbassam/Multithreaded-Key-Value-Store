import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadTest {

    private static final int NUM_CLIENTS = 100;
    private static final int OPS_PER_CLIENT = 6000;
    private static final String HOST = "localhost";
    private static final int PORT = 2020;

    public static void main(String[] args) throws Exception {
        CountDownLatch latch = new CountDownLatch(NUM_CLIENTS);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failureCount = new AtomicInteger(0);

        System.out.println("Starting load test with " + NUM_CLIENTS + " clients, " + OPS_PER_CLIENT + " cycles each...");

        long startTime = System.currentTimeMillis();

        // Spawn concurrent client threads
        for (int i = 0; i < NUM_CLIENTS; i++) {
            int clientId = i;
            Thread.ofVirtual().start(() -> {
                clientTask(clientId, latch, successCount, failureCount);
            });
        }

        // Wait for all clients to finish
        latch.await();

        long elapsed = System.currentTimeMillis() - startTime;
        int totalOps = (successCount.get() + failureCount.get());

        // Final verification: confirm deleted keys are gone
        int verifyFailures = verifyCleanup();

        System.out.println();
        System.out.println("--- Load Test Results ---");
        System.out.println("Clients:        " + NUM_CLIENTS);
        System.out.println("Shards:         " + DataStore.NUM_SHARDS);
        System.out.println("Cycles/client:  " + OPS_PER_CLIENT + " (3 ops each: SET, GET, DEL)");
        System.out.println("Total ops:      " + totalOps);
        System.out.println("Successes:      " + successCount.get());
        System.out.println("Failures:       " + failureCount.get());
        System.out.println("Verify phase:   " + (verifyFailures == 0 ? "PASSED" : verifyFailures + " failures"));
        System.out.println("Time:           " + elapsed + "ms");
        if (elapsed > 0) {
            System.out.println("Throughput:     " + (totalOps * 1000L / elapsed) + " ops/sec");
        }
    }

    private static void clientTask(int clientId, CountDownLatch latch, AtomicInteger successCount, AtomicInteger failureCount) {
        try (Socket socket = new Socket(HOST, PORT)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Read and discard welcome message
            in.readLine();

            for (int i = 0; i < OPS_PER_CLIENT; i++) {
                String key = "client" + clientId + "_key" + i;
                String value = "value" + i;

                // SET
                out.println("SET " + key + " " + value);
                String setResponse = in.readLine();
                if ("OK".equals(setResponse)) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                    System.out.println("[FAIL] Client " + clientId + " SET " + key + " -> " + setResponse);
                }

                // GET and verify correctness
                out.println("GET " + key);
                String getResponse = in.readLine();
                if (value.equals(getResponse)) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                    System.out.println("[FAIL] Client " + clientId + " GET " + key + " expected=" + value + " got=" + getResponse);
                }

                // DEL
                out.println("DEL " + key);
                String delResponse = in.readLine();
                if ("Deleted!".equals(delResponse)) {
                    successCount.incrementAndGet();
                } else {
                    failureCount.incrementAndGet();
                    System.out.println("[FAIL] Client " + clientId + " DEL " + key + " -> " + delResponse);
                }
            }

            // Clean disconnect
            out.println("EXIT");
            in.readLine(); // read "Goodbye!"

        } catch (Exception e) {
            System.out.println("[ERROR] Client " + clientId + ": " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }

    private static int verifyCleanup() {
        int failures = 0;
        try (Socket socket = new Socket(HOST, PORT)) {
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);

            // Read welcome message
            in.readLine();

            // Spot-check that keys from several clients are gone
            for (int clientId = 0; clientId < NUM_CLIENTS; clientId += 5) {
                String key = "client" + clientId + "_key0";
                out.println("GET " + key);
                String response = in.readLine();
                if (!"NOT FOUND".equals(response)) {
                    System.out.println("[VERIFY FAIL] Key " + key + " still exists: " + response);
                    failures++;
                }
            }

            out.println("EXIT");
            in.readLine();

        } catch (Exception e) {
            System.out.println("[VERIFY ERROR] " + e.getMessage());
            failures++;
        }
        return failures;
    }
}
