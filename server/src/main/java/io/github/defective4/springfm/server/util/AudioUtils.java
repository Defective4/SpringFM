package io.github.defective4.springfm.server.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

public class AudioUtils {
    private AudioUtils() {
    }

    public static byte[] createWavHeader(AudioFormat fmt) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        AudioSystem.write(new AudioInputStream(InputStream.nullInputStream(), fmt, 0), AudioFileFormat.Type.WAVE,
                buffer);
        return buffer.toByteArray();
    }
}
