package io.github.defective4.springfm.server.data;

import java.nio.ByteBuffer;
import java.util.zip.CRC32;

import javax.sound.sampled.AudioFormat;

public class SerializableAudioFormat {

    public static class Codec {

        private static final CRC32 CRC = new CRC32();

        private static final byte[] SWITCH_FRAME_HEADER = { 0x57, 0x41, 0x56, 0x45, 0x53, 0x57, 0x49, 0x54, 0x43,
                0x48 };

        public static long calculateChecksum(byte[] data) {
            CRC.reset();
            CRC.update(data, 0, data.length);
            return CRC.getValue();
        }

        public static byte[] createFormatSwitchFrame(AudioFormat target) {
            ByteBuffer buffer = ByteBuffer.allocate(4096);
            buffer.put(SWITCH_FRAME_HEADER);
            buffer.putFloat(target.getSampleRate());
            buffer.put((byte) target.getChannels());
            byte[] bufData = new byte[SWITCH_FRAME_HEADER.length + 6];
            buffer.get(0, bufData);
            buffer.putLong(calculateChecksum(bufData));
            return buffer.array();
        }

        public static AudioFormat fromSwitchFrame(byte[] data) {
            ByteBuffer buffer = ByteBuffer.wrap(data);
            buffer.position(SWITCH_FRAME_HEADER.length);
            return new AudioFormat(buffer.getFloat(), 16, buffer.get(), true, false);
        }

        public static boolean isSwitchFrame(byte[] data) {
            for (int i = 0; i < SWITCH_FRAME_HEADER.length; i++) {
                if (SWITCH_FRAME_HEADER[i] != data[i]) return false;
            }
            CRC.reset();
            CRC.update(data, 0, SWITCH_FRAME_HEADER.length + 6);
            long checksum = CRC.getValue();
            byte[] remoteChecksum = new byte[8];
            System.arraycopy(data, SWITCH_FRAME_HEADER.length + 6, remoteChecksum, 0, remoteChecksum.length);
            return toLong(remoteChecksum) == checksum;
        }

        private static long toLong(byte[] data) {
            long val = 0;
            for (int i = data.length - 1; i >= 0; i--) {
                long shifted = (data[data.length - 1 - i] & 0xffL) << i * 8;
                val |= shifted;
            }
            return val;
        }
    }

    private final int channels;
    private final float sampleRate;

    public SerializableAudioFormat(AudioFormat format) {
        this(format.getSampleRate(), format.getChannels());
    }

    public SerializableAudioFormat(float sampleRate, int channels) {
        this.sampleRate = sampleRate;
        this.channels = channels;
    }

    public int getChannels() {
        return channels;
    }

    public float getSampleRate() {
        return sampleRate;
    }
}
