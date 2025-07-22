package io.github.defective4.springfm.server.util;

public class RateLimiter {
    private int current = 0;
    private long lastTimestamp = 0;
    private final int limit;

    public RateLimiter(int limit) {
        this.limit = limit;
    }

    public void limit(int number) {
        current += number;
        if (current > limit) {
            try {
                Thread.sleep(Math.max(1000 - (System.currentTimeMillis() - lastTimestamp), 0));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            current = 0;
            lastTimestamp = System.currentTimeMillis();
        }
    }

}
