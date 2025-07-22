package io.github.defective4.springfm.server.data;

import java.util.Collections;
import java.util.List;

public class AuthResponse {
    private final List<ProfileInformation> profiles;

    public AuthResponse(List<ProfileInformation> profiles) {
        this.profiles = Collections.unmodifiableList(profiles);
    }

    public List<ProfileInformation> getProfiles() {
        return profiles;
    }

}
