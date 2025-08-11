package io.github.defective4.springfm.backend.config.file;

import java.util.Map;

public class MainConfiguration {
    private final Map<String, ProfileConfiguration> profiles;

    public MainConfiguration(Map<String, ProfileConfiguration> profiles) {
        this.profiles = profiles;
    }

    public Map<String, ProfileConfiguration> getProfiles() {
        return profiles;
    }

    @Override
    public String toString() {
        return "UserConfiguration [profiles=" + profiles + "]";
    }

}
