package io.github.defective4.springfm.server.data;

public class ServiceInformation {
    private final int index;
    private final String name;

    public ServiceInformation(int index, String name) {
        this.index = index;
        this.name = name;
    }

    public int getIndex() {
        return index;
    }

    public String getName() {
        return name;
    }

}
