package io.github.defective4.springfm.backend.config.file;

import java.util.List;

public class ProfileConfiguration {
    private final AudioFormatConfiguration format;
    private final List<ServiceConfiguration> services;

    public ProfileConfiguration(AudioFormatConfiguration format, List<ServiceConfiguration> services) {
        this.format = format;
        this.services = services;
    }

    public AudioFormatConfiguration getFormat() {
        return format;
    }

    public List<ServiceConfiguration> getServices() {
        return services;
    }

    @Override
    public String toString() {
        return "ProfileConfiguration [format=" + format + ", services=" + services + "]";
    }

}
