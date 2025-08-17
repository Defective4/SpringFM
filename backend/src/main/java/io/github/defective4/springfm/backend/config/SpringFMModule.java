package io.github.defective4.springfm.backend.config;

import java.net.URL;
import java.util.Objects;

public class SpringFMModule {
    private final SpringFMModuleInfo moduleInfo;
    private final URL url;

    public SpringFMModule(URL url, SpringFMModuleInfo moduleInfo) {
        this.url = Objects.requireNonNull(url);
        this.moduleInfo = Objects.requireNonNull(moduleInfo);
    }

    public SpringFMModuleInfo getModuleInfo() {
        return moduleInfo;
    }

    public URL getUrl() {
        return url;
    }

}
