package io.github.defective4.springfm.server.service.impl;

import static io.github.defective4.springfm.server.util.DependencyUtils.checkGnuRadio;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.audio.AudioResampler;
import io.github.defective4.springfm.server.audio.AudioSystemAudioResampler;
import io.github.defective4.springfm.server.audio.PassthroughAudioResampler;
import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.service.AdjustableGainService;
import io.github.defective4.springfm.server.service.AnalogRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.DependencyUtils;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class NarrowbandFMService implements AnalogRadioService, AdjustableGainService {

    private boolean debug;
    private final AudioFormat format;
    private float freq;
    private final float freqStep;
    private float gain;
    private DataGenerator generator;
    private DataInputStream inputStream;
    private final float lowerFreq, upperFreq;
    private final String name;
    private DataOutputStream outputStream;
    private Process radioProcess;
    private final AudioResampler resampler;
    private final String sdrParams;
    private Future<?> task;

    public NarrowbandFMService(@ServiceArgument(name = "name") String name,
            @ServiceArgument(name = "lowerFrequency", defaultValue = "137e6") Float lowerFreq,
            @ServiceArgument(name = "upperFrequency", defaultValue = "434e6") Float upperFreq,
            @ServiceArgument(name = "frequencyStep", defaultValue = "5e3") Float freqStep,
            @ServiceArgument(name = "sdrParams", defaultValue = "") String sdrParams, AudioFormat format) {
        this.format = format;
        this.name = name;
        this.lowerFreq = lowerFreq;
        this.upperFreq = upperFreq;
        freq = getMinFrequency();
        this.freqStep = freqStep;
        this.sdrParams = sdrParams;
        resampler = format.getSampleRate() == 48e3f
                ? new PassthroughAudioResampler(sample -> generator.audioSampleGenerated(sample, false))
                : new AudioSystemAudioResampler(new AudioFormat(48e3f, 16, 1, true, false), format,
                        sample -> generator.audioSampleGenerated(sample, true));
    }

    @Override
    public Collection<String> checkMissingDependencies() {
        List<String> missing = new ArrayList<>();
        if (!DependencyUtils.checkPython3()) missing.add("python3");
        if (!checkGnuRadio()) missing.add("gnuradio");
        return Collections.unmodifiableList(missing);
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
        return freqStep;
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
    public void setGain(float gain) throws IOException, IllegalArgumentException {
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
            ns[0] = "-p";
            ns[1] = sdrParams;
            pms = ns;
        }
        radioProcess = ScriptUtils.runGnuRadioScript("nbfm_rx",
                Set.of("pcm_stdout", "pcm_stdout_pcm_writer", "stdin_cmd", "stdin_cmd_cmd_reader"), pms);
        inputStream = new DataInputStream(radioProcess.getInputStream());
        outputStream = new DataOutputStream(radioProcess.getOutputStream());
        resampler.start();
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    inputStream.readFully(buffer);
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
            resampler.stop();
        } finally {
            radioProcess = null;
            inputStream = null;
            outputStream = null;
            task = null;
        }
    }

    @Override
    public void tune(float freq) throws IllegalArgumentException, IOException {
        this.freq = freq;
    }

}
