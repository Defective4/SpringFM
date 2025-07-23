package io.github.defective4.springfm.server.processing.impl;

import java.io.IOException;
import java.io.OutputStream;

import io.github.defective4.sdr.rds.RDSAdapter;
import io.github.defective4.sdr.rds.RDSReceiver;
import io.github.defective4.springfm.server.data.AnnotationGenerator;
import io.github.defective4.springfm.server.data.AudioAnnotation;
import io.github.defective4.springfm.server.processing.StreamingAnnotationProcessor;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class GnuRadioRDSProcessor implements StreamingAnnotationProcessor {

    private final AnnotationGenerator generator;
    private Process process;
    private OutputStream processStream;
    private final int rdsPort;
    private RDSReceiver rdsRx;

    public GnuRadioRDSProcessor(AnnotationGenerator generator, int rdsPort) {
        this.rdsPort = rdsPort;
        this.generator = generator;
    }

    @Override
    public boolean isStarted() {
        return process != null && process.isAlive() && processStream != null && rdsRx != null;
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        process = ScriptUtils.runGnuRadioScript("rds_processor", "-p", Integer.toString(rdsPort));
        processStream = process.getOutputStream();
        rdsRx = new RDSReceiver("tcp://127.0.0.1:" + rdsPort, false);
        ThreadUtils.submit(() -> {
            rdsRx.addListener(new RDSAdapter() {
                private String lastText, lastTitle;

                @Override
                public void radiotextUpdated(String radiotext) {
                    lastText = radiotext;
                    generator.provide(new AudioAnnotation(lastTitle, lastText));
                }

                @Override
                public void stationUpdated(String station) {
                    lastTitle = station;
                    generator.provide(new AudioAnnotation(lastTitle, lastText));
                }
            });
            try {
                rdsRx.start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    @Override
    public void stop() throws IOException {
        if (!isStarted()) throw new IllegalStateException("Already stopped");
        try {
            process.destroyForcibly();
            processStream.close();
            rdsRx.close();
        } finally {
            process = null;
            processStream = null;
            rdsRx = null;
        }
    }

    @Override
    public void write(byte[] data, int len) throws IOException {
        if (!isStarted()) throw new IllegalStateException("GnuRadio process is not started");
        processStream.write(data, 0, len);
    }

}
