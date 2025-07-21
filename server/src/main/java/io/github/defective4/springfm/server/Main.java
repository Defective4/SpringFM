package io.github.defective4.springfm.server;

import java.io.FileInputStream;
import java.io.InputStream;

import io.github.defective4.springfm.server.service.TestService;

public class Main {
    public static void main(String[] args) {
        try (InputStream ais = new FileInputStream("wbfm.wav")) {
            TestService service = new TestService(ais, packet -> {
                if (packet.getType() == 1) {
                    System.out.println(new String(packet.getPayload()));
                }
            });
            service.start();

            Object lock = new Object();
            synchronized (lock) {
                lock.wait();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
