package io.github.defective4.springfm.server.service.impl;

import static io.github.defective4.springfm.server.util.DependencyUtils.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import io.github.defective4.springfm.server.audio.AudioResampler;
import io.github.defective4.springfm.server.audio.AudioSystemAudioResampler;
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

public class RtlBroadcastFMService implements AnalogRadioService, AdjustableGainService {

    private boolean debug;
    private DataInputStream fmInput;
    private final AudioFormat format;

    private float frequency;
    private float gain;
    private DataGenerator generator;
    private final float maxFrequency;
    private final float minFrequency;
    private final String name;
    private final AudioAnnotationProcessor processor;
    private final RDSProcessor processorType;
    private final AudioResampler resampler;

    private Process rtlFm;
    private final String rtlFmPath;
    private Future<?> task;

    public RtlBroadcastFMService(@ServiceArgument(name = "name", defaultValue = "RTL-SDR Broadcast FM") String name,
            @ServiceArgument(name = "rtlFmPath", defaultValue = "rtl_fm") String rtlFmPath,
            @ServiceArgument(name = "minFrequency", defaultValue = "87e6") Double minFrequency,
            @ServiceArgument(name = "maxFrequency", defaultValue = "108e6") Double maxFrequency,
            @ServiceArgument(name = "rdsProcessor", defaultValue = "REDSEA") RDSProcessor processorType,
            @ServiceArgument(name = "rdsArgs", defaultValue = "-1") Double rdsArgs, AudioFormat format) {
        this.processorType = processorType;
        if (format.getChannels() != 1) throw new IllegalArgumentException("Only mono audio format is allowed");
        this.name = name;
        this.format = format;
        this.rtlFmPath = rtlFmPath;
        this.minFrequency = Objects.requireNonNull((float) (double) minFrequency);
        this.maxFrequency = Objects.requireNonNull((float) (double) maxFrequency);
        AnnotationGenerator ag = annotation -> {
            if (generator != null) generator.packetGenerated(new Packet(new AudioAnnotationPayload(annotation)));
        };
        processor = switch (processorType) {
            case GR_RDS -> new GnuRadioRDSProcessor(ag, (int) (double) rdsArgs);
            case REDSEA -> new RedseaRDSProcessor(ag);
            default -> null;
        };
        frequency = getMinFrequency();
        resampler = new AudioSystemAudioResampler(new AudioFormat(171e3f, 16, 1, true, false), format, (data) -> {
            generator.audioSampleGenerated(data, true);
        });
    }

    @Override
    public Collection<String> checkMissingDependencies() {
        List<String> missing = new ArrayList<>();
        if (!checkRtlFm(rtlFmPath)) missing.add("rtl_fm");
        switch (processorType) {
            case REDSEA -> {
                if (!checkRedsea()) missing.add("redsea");
            }
            case GR_RDS -> {
                if (!checkPython3()) missing.add("python3");
                if (!checkGnuRadio()) missing.add("gnuradio");
                if (!checkGnuRadioRDS()) missing.add("gr-rds");
            }
            default -> {}
        }
        return Collections.unmodifiableList(missing);
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
    public void init() throws IOException {
    }

    @Override
    public boolean isStarted() {
        return rtlFm != null && rtlFm.isAlive() && task != null && !task.isCancelled();
    }

    @Override
    public void setDebugMode(boolean debug) {
        this.debug = debug;
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
            if (processor != null && processor.isStarted()) processor.stop();
            resampler.stop();
        } finally {
            rtlFm = null;
            fmInput = null;
            task = null;
        }
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        if (processor != null) processor.start();
        rtlFm = ScriptUtils.startProcess(rtlFmPath, new Object[] { "-f", (int) frequency, "-M", "fm", "-s", "171k",
                "-g", gain, "-E", "deemp", "-F", "9", "-A", "fast", "-" });
        fmInput = new DataInputStream(rtlFm.getInputStream());
        task = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    fmInput.readFully(buffer);
                    resampler.write(buffer);
                    if (processor != null) processor.writeAudioSample(buffer, buffer.length);
                }
            } catch (Exception e) {
                if (debug) e.printStackTrace();
            }
        });
    }
}
