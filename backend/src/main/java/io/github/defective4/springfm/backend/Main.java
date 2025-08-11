package io.github.defective4.springfm.backend;

import io.github.defective4.springfm.backend.config.ConfigurationReader;
import io.github.defective4.springfm.backend.web.ProfileController;
import io.javalin.http.Context;

public class Main {
    public static void main(String[] args) {
        try {
            ProfileController controller = new ProfileController(new ConfigurationReader());
            controller.start(8080);
            Context ctx;

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
