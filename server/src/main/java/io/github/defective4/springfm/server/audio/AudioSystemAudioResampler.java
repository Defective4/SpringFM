package io.github.defective4.springfm.server.audio;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import io.github.defective4.springfm.server.util.ThreadUtils;

public class AudioSystemAudioResampler implements AudioResampler {
    private final AudioFormat from, to;
    private final AudioSampleGenerator generator;
    private DataInputStream input;
    private OutputStream output;
    private Future<?> resamplerTask, readerTask;

    public AudioSystemAudioResampler(AudioFormat from, AudioFormat to, AudioSampleGenerator generator) {
        this.from = from;
        this.to = to;
        this.generator = generator;
    }

    @SuppressWarnings("resource")
    @Override
    public void start() throws IOException {
        PipedInputStream audioIn = new PipedInputStream(
                (int) (from.getSampleRate() * (from.getSampleSizeInBits() / 8)));
        PipedOutputStream audioOut = new PipedOutputStream();
        output = new PipedOutputStream(audioIn);
        input = new DataInputStream(
                new PipedInputStream(audioOut, (int) (to.getSampleRate() * (to.getSampleSizeInBits() / 8))));

        resamplerTask = ThreadUtils.submit(() -> {
            try {
                AudioInputStream source = new AudioInputStream(audioIn, from, Long.MAX_VALUE);
                AudioInputStream target = AudioSystem.getAudioInputStream(to, source);
                AudioSystem.write(target, AudioFileFormat.Type.AU, audioOut);
            } catch (Exception e) {}
        });

        readerTask = ThreadUtils.submit(() -> {
            try {
                byte[] data = new byte[4096];
                while (true) {
                    input.readFully(data);
                    generator.sampleGenerated(data);
                }
            } catch (Exception e2) {}
        });
    }

    @Override
    public void stop() throws IOException {
        try {
            if (resamplerTask != null) resamplerTask.cancel(true);
            if (readerTask != null) readerTask.cancel(true);
            if (output != null) output.close();
            if (input != null) input.close();
        } finally {
            resamplerTask = null;
            readerTask = null;
            output = null;
            input = null;
        }
    }

    @Override
    public synchronized void write(byte[] data) throws IOException {
        if (output != null) output.write(data);
    }
}
