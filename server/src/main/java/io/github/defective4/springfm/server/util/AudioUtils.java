package io.github.defective4.springfm.server.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

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
        byte[] data = buffer.toByteArray();
        byte[] intData = new byte[4];
        Arrays.fill(intData, Byte.MAX_VALUE);
        System.arraycopy(intData, 0, data, 4, intData.length);
        System.arraycopy(intData, 0, data, data.length - 4, intData.length);
        return data;
    }
}
