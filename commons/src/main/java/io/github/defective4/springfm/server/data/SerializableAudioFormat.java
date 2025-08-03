package io.github.defective4.springfm.server.data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import javax.sound.sampled.AudioFormat;

public class SerializableAudioFormat {

    public static class Codec {
        public static SerializableAudioFormat fromStream(DataInputStream stream) throws IOException {
            return new SerializableAudioFormat(stream.readFloat(), stream.readByte(), stream.readByte(),
                    stream.readBoolean());
        }

        public static void toStream(SerializableAudioFormat format, DataOutputStream stream) throws IOException {
            stream.writeFloat(format.sampleRate);
            stream.writeByte(format.sampleSize);
            stream.writeByte(format.channels);
            stream.writeBoolean(format.bigEndian);
        }
    }

    private final boolean bigEndian;
    private final int channels;
    private final float sampleRate;
    private final int sampleSize;

    public SerializableAudioFormat(AudioFormat format) {
        this(format.getSampleRate(), format.getSampleSizeInBits(), format.getChannels(), format.isBigEndian());
    }

    public SerializableAudioFormat(float sampleRate, int sampleSize, int channels, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSize = sampleSize;
        this.channels = channels;
        this.bigEndian = bigEndian;
    }

    public int getChannels() {
        return channels;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSize() {
        return sampleSize;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

}
