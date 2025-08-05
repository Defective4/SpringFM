package io.github.defective4.springfm.backend.config.file;

import java.util.Map;

public class ServiceConfiguration {
    private final Map<String, Object> args;
    private final String className;

    public ServiceConfiguration(String className, Map<String, Object> args) {
        this.className = className;
        this.args = args;
    }

    public Map<String, Object> getArgs() {
        return args;
    }

    public String getClassName() {
        return className;
    }

    @Override
    public String toString() {
        return "ServiceConfiguration [args=" + args + ", className=" + className + "]";
    }

}
