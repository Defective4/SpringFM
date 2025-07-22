package io.github.defective4.springfm.server.packet.impl;

import java.util.Objects;

import io.github.defective4.springfm.server.data.AudioAnnotation;
import io.github.defective4.springfm.server.packet.PacketPayload;

public class AudioAnnotationPayload extends PacketPayload {

    public AudioAnnotationPayload(AudioAnnotation annotation) {
        super(Objects.requireNonNull(annotation));
    }

}
