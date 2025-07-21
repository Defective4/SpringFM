package io.github.defective4.springfm.server.data;

@FunctionalInterface
public interface AnnotationGenerator {
    void provide(AudioAnnotation annotation);
}
