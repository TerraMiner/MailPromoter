package ua.terra;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Executor {
    private final ExecutorService executorService;

    public Executor(int threads) {
        executorService = Executors.newFixedThreadPool(threads);
    }

    public void execute(Runnable action) {
        executorService.submit(action);
    }

    public void close() {
        executorService.shutdownNow();
    }

}
