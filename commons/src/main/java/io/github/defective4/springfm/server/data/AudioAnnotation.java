package io.github.defective4.springfm.server.data;

import java.util.Objects;

public class AudioAnnotation {
    private final String title, description;

    public AudioAnnotation(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if ((obj == null) || (getClass() != obj.getClass())) return false;
        AudioAnnotation other = (AudioAnnotation) obj;
        return Objects.equals(description, other.description) && Objects.equals(title, other.title);
    }

    public String getDescription() {
        return description;
    }

    public String getTitle() {
        return title;
    }

    @Override
    public int hashCode() {
        return Objects.hash(description, title);
    }

    @Override
    public String toString() {
        return "AudioAnnotation [title=" + title + ", description=" + description + "]";
    }

}
