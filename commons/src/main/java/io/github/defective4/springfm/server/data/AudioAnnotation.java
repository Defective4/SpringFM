package io.github.defective4.springfm.server.data;

public class AudioAnnotation {
    private final String title, description;

    public AudioAnnotation(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public String toString() {
        return "AudioAnnotation [title=" + title + ", description=" + description + "]";
    }

}
