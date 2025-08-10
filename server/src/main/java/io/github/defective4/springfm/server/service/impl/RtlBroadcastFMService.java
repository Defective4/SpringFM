package io.github.defective4.springfm.server.service.impl;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.audio.AudioResampler;
import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class RtlBroadcastFMService implements AnalogRadioService {

    private DataInputStream fmInput;
    private final AudioFormat format;

    private float frequency;
    private DataGenerator generator;
    private final String name;
    private final AudioResampler resampler;
    private Process rtlFm;
    private final String rtlFmPath;
    private Future<?> task;

    public RtlBroadcastFMService(@ServiceArgument(name = "name") String name,
            @ServiceArgument(name = "rtlFmPath", defaultValue = "rtl_fm") String rtlFmPath, AudioFormat format) {
        this.name = Objects.requireNonNull(name);
        this.format = format;
        this.rtlFmPath = rtlFmPath;
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
    public float getFrequencyStep() {
        return 100e3f;
    }

    @Override
    public float getMaxFrequency() {
        return 108e6f; // TODO
    }

    @Override
    public float getMinFrequency() {
        return 87e6f; // TODO
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
            resampler.stop();
        } finally {
            rtlFm = null;
            fmInput = null;
            task = null;
        }
    }

    @Override
    public void tune(float freq) throws IllegalArgumentException, IOException {
        if (freq < getMinFrequency() || freq > getMaxFrequency())
            throw new IllegalArgumentException("Frequency out of bounds");
        frequency = freq;
        tuneRtlFm();
    }

    private void tuneRtlFm() throws IOException {
        stop();
        resampler.start();
        rtlFm = ScriptUtils.startProcess(rtlFmPath,
                new Object[] { "-f", (int) frequency, "-M", "fm", "-s", "171k", "-g", "24", // TODO gain
                        "-E", "deemp", "-F", "9", "-A", "fast", "-" });
        fmInput = new DataInputStream(rtlFm.getInputStream());
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    fmInput.readFully(buffer);
                    resampler.write(buffer);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

}
