package io.github.defective4.springfm.server.data;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

public class AuthResponse {
    private final String hashAlgo;
    private final String instanceName;
    private final List<ProfileInformation> profiles;

    public AuthResponse(List<ProfileInformation> profiles, String instanceName, String hashAlgo) {
        this.instanceName = instanceName;
        this.hashAlgo = hashAlgo;
        this.profiles = Collections.unmodifiableList(profiles);
    }

    public String getHashAlgo() {
        return hashAlgo;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public List<ProfileInformation> getProfiles() {
        return profiles;
    }

    public byte[] hash(MessageDigest md) {
        for (ProfileInformation info : profiles) {
            md.update(info.getName().getBytes(StandardCharsets.UTF_8));
            for (ServiceInformation service : info.getServices()) {
                ByteBuffer dataBuffer = ByteBuffer.allocate(17);
                dataBuffer.put(service.getTuningType());
                AnalogTuningInformation analog = service.getAnalogTuning();
                if (analog != null) {
                    dataBuffer.putFloat(analog.getMinFreq());
                    dataBuffer.putFloat(analog.getMaxFreq());
                    dataBuffer.putFloat(analog.getStep());
                }

                GainInformation gain = service.getGainInfo();
                dataBuffer.putFloat(gain.isGainSupported() ? gain.getMaxGain() : -1);
                md.update(dataBuffer.array());
                DigitalTuningInformation digital = service.getDigitalTuning();
                if (digital != null)
                    for (String station : digital.getStations()) md.update(station.getBytes(StandardCharsets.UTF_8));
            }
        }
        return md.digest();
    }

    @Override
    public String toString() {
        return "AuthResponse [instanceName=" + instanceName + ", profiles=" + profiles + ", hashAlgo=" + hashAlgo + "]";
    }

    public boolean verify(MessageDigest md, byte[] hash) {
        byte[] newHash = hash(md);
        if (newHash.length != hash.length) return false;
        for (int i = 0; i < hash.length; i++) {
            if (hash[i] != newHash[i]) return false;
        }
        return true;
    }

}
