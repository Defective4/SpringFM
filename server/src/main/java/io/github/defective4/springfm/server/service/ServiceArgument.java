package io.github.defective4.springfm.server.service;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ServiceArgument {
    String defaultValue() default "";

    String name();
}
