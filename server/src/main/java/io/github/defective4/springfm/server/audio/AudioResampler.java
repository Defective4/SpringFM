package io.github.defective4.springfm.server.audio;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import io.github.defective4.springfm.server.util.ThreadUtils;

public class AudioResampler {
    private final AudioFormat from, to;
    private final AudioSampleGenerator generator;
    private InputStream input;
    private OutputStream output;
    private Future<?> resamplerTask, readerTask;

    public AudioResampler(AudioFormat from, AudioFormat to, AudioSampleGenerator generator) {
        this.from = from;
        this.to = to;
        this.generator = generator;
    }

    public void start() throws IOException {
        PipedInputStream audioIn = new PipedInputStream(
                (int) (from.getSampleRate() * (from.getSampleSizeInBits() / 8)));
        PipedOutputStream audioOut = new PipedOutputStream();
        output = new PipedOutputStream(audioIn);
        input = new PipedInputStream(audioOut, (int) (to.getSampleRate() * (to.getSampleSizeInBits() / 8)));

        resamplerTask = ThreadUtils.submit(() -> {
            try {
                AudioInputStream source = new AudioInputStream(audioIn, from, Long.MAX_VALUE);
                AudioInputStream target = AudioSystem.getAudioInputStream(to, source);
                AudioSystem.write(target, AudioFileFormat.Type.AU, audioOut);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        readerTask = ThreadUtils.submit(() -> {
            try {
                while (true) {
                    if (input.available() > 0) {
                        byte[] data = new byte[input.available()];
                        int read = input.read(data);
                        generator.sampleGenerated(data, read);
                    }
                }
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        });
    }

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

    public synchronized void write(byte[] data) throws IOException {
        if (output != null) output.write(data);
    }
}
