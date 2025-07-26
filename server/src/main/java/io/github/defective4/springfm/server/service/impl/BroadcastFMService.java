package io.github.defective4.springfm.server.service.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.Future;

import io.github.defective4.springfm.server.data.AnnotationGenerator;
import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.StreamingAnnotationProcessor;
import io.github.defective4.springfm.server.processing.impl.GnuRadioRDSProcessor;
import io.github.defective4.springfm.server.processing.impl.RedseaRDSProcessor;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class BroadcastFMService implements AnalogRadioService {

    private float freq;
    private DataGenerator generator;
    private DataInputStream inputStream;
    private final float lowerFreq, upperFreq;

    private final String name;
    private DataOutputStream outputStream;
    private Process radioProcess;
    private final StreamingAnnotationProcessor rdsProcessor;

    private final String sdrParams;

    private Future<?> task;

    public BroadcastFMService(String name, float lowerFreq, float upperFreq, String sdrParams, boolean useRedsea,
            int grRdsPort) {
        this.name = name;
        this.lowerFreq = lowerFreq;
        this.upperFreq = upperFreq;
        this.sdrParams = sdrParams;
        freq = getMinFrequency();
        AnnotationGenerator annotationGenerator = annotation -> {
            generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
        };
        rdsProcessor = useRedsea ? new RedseaRDSProcessor(annotationGenerator)
                : new GnuRadioRDSProcessor(annotationGenerator, grRdsPort);
    }

    @Override
    public float getCurrentFrequency() {
        return freq;
    }

    @Override
    public float getFrequencyStep() {
        return 100e3f;
    }

    @Override
    public float getMaxFrequency() {
        return upperFreq;
    }

    @Override
    public float getMinFrequency() {
        return lowerFreq;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isStarted() {
        return radioProcess != null && radioProcess.isAlive();
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        stop();
        String[] pms = new String[] { "-f", Float.toString(freq) };
        if (sdrParams != null) {
            String[] ns = new String[pms.length + 2];
            System.arraycopy(pms, 0, ns, 2, pms.length);
            ns[0] = "-s";
            ns[1] = sdrParams;
            pms = ns;
        }
        radioProcess = ScriptUtils.runGnuRadioScript("wbfm_rx", Set.of("cmd_reader", "pcm_writer"), pms);
        inputStream = new DataInputStream(radioProcess.getInputStream());
        outputStream = new DataOutputStream(radioProcess.getOutputStream());
        rdsProcessor.start();
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    inputStream.readFully(buffer);
                    rdsProcessor.write(buffer, buffer.length);
                    generator.audioSampleGenerated(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    stop();
                } catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
        });
    }

    @Override
    public void stop() throws IOException {
        try {
            if (radioProcess != null) radioProcess.destroyForcibly();
            if (task != null) task.cancel(true);
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (rdsProcessor.isStarted()) rdsProcessor.stop();
        } finally {
            radioProcess = null;
            inputStream = null;
            outputStream = null;
            task = null;
        }
    }

    @Override
    public synchronized void tune(float freq) throws IllegalArgumentException, IOException {
        outputStream.writeFloat(1);
        outputStream.writeFloat(freq);
        outputStream.flush();
        this.freq = freq;
    }

}
