package io.github.defective4.springfm.server.data;

import java.util.Collections;
import java.util.List;

public class AuthResponse {
    private final String instanceName;
    private final List<ProfileInformation> profiles;

    public AuthResponse(List<ProfileInformation> profiles, String instanceName) {
        this.instanceName = instanceName;
        this.profiles = Collections.unmodifiableList(profiles);
    }

    public String getInstanceName() {
        return instanceName;
    }

    public List<ProfileInformation> getProfiles() {
        return profiles;
    }

}
