package io.github.defective4.springfm.server.data;

import java.util.List;

public class ProfileInformation {
    private final String name;
    private final List<ServiceInformation> services;

    public ProfileInformation(String name, List<ServiceInformation> services) {
        this.name = name;
        this.services = services;
    }

    public String getName() {
        return name;
    }

    public List<ServiceInformation> getServices() {
        return services;
    }

}
