package io.github.defective4.springfm.server.data;

@FunctionalInterface
public interface AnnotationProvider {
    void provide(AudioAnnotation annotation);
}
