package io.github.defective4.springfm.server.service.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.audio.AudioResampler;
import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.packet.Packet;
import io.github.defective4.springfm.server.packet.impl.AudioAnnotationPayload;
import io.github.defective4.springfm.server.processing.StreamingAnnotationProcessor;
import io.github.defective4.springfm.server.processing.impl.RedseaRDSProcessor;
import io.github.defective4.springfm.server.service.AdjustableGainService;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class RtlBroadcastFMService implements AnalogRadioService, AdjustableGainService {

    private DataInputStream fmInput;
    private final AudioFormat format;
    private float frequency;

    private float gain;
    private DataGenerator generator;
    private final float maxFrequency;
    private final float minFrequency;
    private final String name;
    private final StreamingAnnotationProcessor processor;
    private final AudioResampler resampler;
    private Process rtlFm;
    private final String rtlFmPath;

    private Future<?> task;

    public RtlBroadcastFMService(@ServiceArgument(name = "name") String name,
            @ServiceArgument(name = "rtlFmPath", defaultValue = "rtl_fm") String rtlFmPath,
            @ServiceArgument(name = "minFrequency", defaultValue = "87e6") Float minFrequency,
            @ServiceArgument(name = "maxFrequency", defaultValue = "108e6") Float maxFrequency, AudioFormat format) {
        this.name = Objects.requireNonNull(name);
        this.format = format;
        this.rtlFmPath = rtlFmPath;
        this.minFrequency = minFrequency;
        this.maxFrequency = maxFrequency;
        processor = new RedseaRDSProcessor(annotation -> {
            if (generator != null) generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
        }); // TODO
        frequency = getMinFrequency();
        resampler = new AudioResampler(new AudioFormat(171e3f, 16, 1, true, false), format, (data, len) -> {
            byte[] effective;
            if (data.length == len) {
                effective = data;
            } else {
                effective = Arrays.copyOf(data, len);
            }
            generator.audioSampleGenerated(effective, true);
        });
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public float getCurrentFrequency() {
        return frequency;
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
        return maxFrequency;
    }

    @Override
    public float getMaxGain() {
        return 49.6f;
    }

    @Override
    public float getMinFrequency() {
        return minFrequency;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean isStarted() {
        return rtlFm != null && rtlFm.isAlive() && task != null && !task.isCancelled();
    }

    @Override
    public void setGain(float gain) throws IOException, IllegalArgumentException {
        this.gain = gain;
        tuneRtlFm();
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        tuneRtlFm();
    }

    @Override
    public void stop() throws IOException {
        try {
            if (rtlFm != null) rtlFm.destroyForcibly();
            if (fmInput != null) fmInput.close();
            if (task != null) task.cancel(true);
            if (processor.isStarted()) processor.stop();
            resampler.stop();
        } finally {
            rtlFm = null;
            fmInput = null;
            task = null;
        }
    }

    @Override
    public void tune(float freq) throws IllegalArgumentException, IOException {
        frequency = freq;
        tuneRtlFm();
    }

    private void tuneRtlFm() throws IOException {
        stop();
        resampler.start();
        processor.start();
        rtlFm = ScriptUtils.startProcess(rtlFmPath,
                new Object[] { "-f", (int) frequency, "-M", "fm", "-s", "171k", "-g", gain, // TODO gain
                        "-E", "deemp", "-F", "9", "-A", "fast", "-" });
        fmInput = new DataInputStream(rtlFm.getInputStream());
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    fmInput.readFully(buffer);
                    resampler.write(buffer);
                    processor.write(buffer, buffer.length);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
