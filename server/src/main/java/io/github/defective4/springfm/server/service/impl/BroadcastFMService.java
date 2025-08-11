package io.github.defective4.springfm.server.service.impl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.audio.AudioResampler;
import io.github.defective4.springfm.server.data.AnnotationGenerator;
import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.AudioAnnotationProcessor;
import io.github.defective4.springfm.server.processing.RDSProcessor;
import io.github.defective4.springfm.server.processing.impl.GnuRadioRDSProcessor;
import io.github.defective4.springfm.server.processing.impl.RedseaRDSProcessor;
import io.github.defective4.springfm.server.service.AdjustableGainService;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class BroadcastFMService implements AnalogRadioService, AdjustableGainService {

    private boolean debug;
    private final AudioFormat format;
    private float freq;
    private float gain;

    private DataGenerator generator;
    private DataInputStream inputStream;
    private final float lowerFreq, upperFreq;
    private final String name;

    private DataOutputStream outputStream;

    private Process radioProcess;

    private final AudioAnnotationProcessor rdsProcessor;
    private final AudioResampler resampler;
    private final String sdrParams;
    private Future<?> task;

    public BroadcastFMService(@ServiceArgument(name = "name") String name,
            @ServiceArgument(name = "lowerFreq", defaultValue = "88e6") Double lowerFreq,
            @ServiceArgument(name = "upperFreq", defaultValue = "108e6") Double upperFreq,
            @ServiceArgument(name = "sdrParams") String sdrParams,
            @ServiceArgument(name = "rdsProcessor", defaultValue = "REDSEA") RDSProcessor processorType,
            @ServiceArgument(name = "rdsArgs", defaultValue = "-1") Double rdsArgs, AudioFormat format) {
        this.format = Objects.requireNonNull(format);
        this.name = Objects.requireNonNull(name);
        this.lowerFreq = (float) (double) Objects.requireNonNull(lowerFreq);
        this.upperFreq = (float) (double) Objects.requireNonNull(upperFreq);
        this.sdrParams = sdrParams;
        resampler = new AudioResampler(new AudioFormat(171e3f, 16, 1, true, false), format, (data) -> {
            generator.audioSampleGenerated(data, true);
        });
        freq = getMinFrequency();
        AnnotationGenerator annotationGenerator = annotation -> {
            generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
        };
        rdsProcessor = switch (processorType) {
            case GR_RDS -> new GnuRadioRDSProcessor(annotationGenerator, (int) (double) rdsArgs);
            case REDSEA -> new RedseaRDSProcessor(annotationGenerator);
            default -> null;
        };
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public float getCurrentFrequency() {
        return freq;
    }

    @Override
    public float getCurrentGain() {
        return gain;
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
    public float getMaxGain() {
        return 49.6f;
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
    public void setDebugMode(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setGain(float gain) throws IOException {
        outputStream.writeFloat(2);
        outputStream.writeFloat(gain);
        outputStream.flush();
        this.gain = gain;
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        stop();
        String[] pms = new String[] { "-f", Float.toString(freq), "-g", Float.toString(gain) };
        if (sdrParams != null) {
            String[] ns = new String[pms.length + 2];
            System.arraycopy(pms, 0, ns, 2, pms.length);
            ns[0] = "-s";
            ns[1] = sdrParams;
            pms = ns;
        }
        radioProcess = ScriptUtils.runGnuRadioScript("wbfm_rx",
                Set.of("pcm_stdout", "pcm_stdout_pcm_writer", "stdin_cmd", "stdin_cmd_cmd_reader"), pms);
        inputStream = new DataInputStream(radioProcess.getInputStream());
        outputStream = new DataOutputStream(radioProcess.getOutputStream());
        if (rdsProcessor != null) rdsProcessor.start();
        resampler.start();
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    inputStream.readFully(buffer);
                    if (rdsProcessor != null) rdsProcessor.writeAudioSample(buffer, buffer.length);
                    resampler.write(buffer);
                }
            } catch (Exception e) {
                if (debug) e.printStackTrace();
            } finally {
                try {
                    stop();
                } catch (Exception e2) {
                    if (debug) e2.printStackTrace();
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
            if (rdsProcessor != null && rdsProcessor.isStarted()) rdsProcessor.stop();
            resampler.stop();
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
