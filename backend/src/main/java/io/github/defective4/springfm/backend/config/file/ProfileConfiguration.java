package io.github.defective4.springfm.backend.config.file;

import java.util.List;

public class ProfileConfiguration {
    private final List<ServiceConfiguration> services;

    public ProfileConfiguration(List<ServiceConfiguration> services) {
        this.services = services;
    }

    public List<ServiceConfiguration> getServices() {
        return services;
    }

    @Override
    public String toString() {
        return "ProfileConfiguration [services=" + services + "]";
    }

}
