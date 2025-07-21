package io.github.defective4.springfm.server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import io.github.defective4.springfm.server.service.TestService;

public class Main {
    public static void main(String[] args) {
        try (InputStream ais = new FileInputStream("wbfm.wav"); OutputStream out = new FileOutputStream("data.wav")) {
            TestService service = new TestService(ais, packet -> {
                if (packet.getType() == 2) {
                    try {
                        out.write(packet.getPayload());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
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
