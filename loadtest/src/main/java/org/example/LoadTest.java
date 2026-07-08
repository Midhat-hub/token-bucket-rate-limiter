package org.example;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
public class LoadTest {

    public static void main(String[] args) throws InterruptedException {
        int totalRequests = 2000;
        String[] baseUrls = {"http://localhost:8080", "http://localhost:8081"};

        HttpClient client = HttpClient.newHttpClient();
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

        AtomicInteger allowCount = new AtomicInteger(0);
        AtomicInteger denyCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(totalRequests);

        long startTime = System.currentTimeMillis();
        Semaphore concurrencyLimiter = new Semaphore(150); // max 150 in-flight requests at once
        for (int i = 0; i < totalRequests; i++) {
            String baseUrl = baseUrls[i % baseUrls.length]; // alternate between 8080 and 8081
            String url = baseUrl + "/check/testclient";

            executor.submit(() -> {
                try {
                    concurrencyLimiter.acquire();
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .POST(HttpRequest.BodyPublishers.noBody())
                                .build();

                        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

                        if (response.statusCode() == 200) {
                            allowCount.incrementAndGet();
                        } else if (response.statusCode() == 429) {
                            denyCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    } finally {
                        concurrencyLimiter.release();
                    }
                } catch (Exception e) {
                    errorCount.incrementAndGet();
                    System.out.println("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        long endTime = System.currentTimeMillis();
        executor.shutdown();

        double durationSeconds = (endTime - startTime) / 1000.0;

        System.out.println("=== Distributed Load Test Results ===");
        System.out.println("Total requests: " + totalRequests + " split across " + baseUrls.length + " instances");
        System.out.println("ALLOW: " + allowCount.get());
        System.out.println("DENY: " + denyCount.get());
        System.out.println("Errors: " + errorCount.get());
        System.out.println("Duration: " + durationSeconds + " seconds");
        System.out.println("Requests/sec: " + (totalRequests / durationSeconds));
    }
}
//import java.net.URI;
//import java.net.http.HttpClient;
//import java.net.http.HttpRequest;
//import java.net.http.HttpResponse;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.atomic.AtomicInteger;
//import java.util.concurrent.CountDownLatch;
//
//public class LoadTest {
//
//    public static void main(String[] args) throws InterruptedException {
//        int totalRequests = 10000;
//        String[] clientIds = {"loadtest1", "loadtest2", "loadtest3", "loadtest4", "loadtest5"};
//
//        HttpClient client = HttpClient.newHttpClient();
//        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
//
//        AtomicInteger allowCount = new AtomicInteger(0);
//        AtomicInteger denyCount = new AtomicInteger(0);
//        AtomicInteger errorCount = new AtomicInteger(0);
//
//        CountDownLatch latch = new CountDownLatch(totalRequests);
//
//        long startTime = System.currentTimeMillis();
//
//        for (int i = 0; i < totalRequests; i++) {
//            String clientId = clientIds[i % clientIds.length]; // round-robin across clients
//            String url = "http://localhost:8080/check/" + clientId;
//
//            executor.submit(() -> {
//                try {
//                    HttpRequest request = HttpRequest.newBuilder()
//                            .uri(URI.create(url))
//                            .POST(HttpRequest.BodyPublishers.noBody())
//                            .build();
//
//                    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
//
//                    if (response.statusCode() == 200) {
//                        allowCount.incrementAndGet();
//                    } else if (response.statusCode() == 429) {
//                        denyCount.incrementAndGet();
//                    } else {
//                        errorCount.incrementAndGet();
//                    }
//                } catch (Exception e) {
//                    errorCount.incrementAndGet();
//                } finally {
//                    latch.countDown();
//                }
//            });
//        }
//
//        latch.await();
//        long endTime = System.currentTimeMillis();
//        executor.shutdown();
//
//        double durationSeconds = (endTime - startTime) / 1000.0;
//
//        System.out.println("=== Load Test Results ===");
//        System.out.println("Total requests: " + totalRequests);
//        System.out.println("Clients used: " + clientIds.length);
//        System.out.println("ALLOW: " + allowCount.get());
//        System.out.println("DENY: " + denyCount.get());
//        System.out.println("Errors: " + errorCount.get());
//        System.out.println("Duration: " + durationSeconds + " seconds");
//        System.out.println("Requests/sec: " + (totalRequests / durationSeconds));
//    }
//}