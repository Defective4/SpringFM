package io.github.defective4.springfm.server.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ThreadUtils {
    private static final ExecutorService service = Executors.newVirtualThreadPerTaskExecutor();

    public static void shutdown() {
        service.shutdownNow();
    }

    public static Future<?> submit(Runnable runnable) {
        return service.submit(runnable);
    }
}
