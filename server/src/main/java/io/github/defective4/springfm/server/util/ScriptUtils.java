package io.github.defective4.springfm.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public class ScriptUtils {
    private ScriptUtils() {
    }

    public static Process runGnuRadioScript(String resource, String... args) throws IOException {
        File tmpDir = Files.createTempDirectory("springfm").toFile();
        File scriptFile = new File(tmpDir, resource);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scriptFile.delete();
            if (tmpDir.isDirectory() && tmpDir.list().length == 0) tmpDir.delete();
        }));
        try (InputStream in = ScriptUtils.class.getResourceAsStream("/" + resource + ".py")) {
            Files.copy(in, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        String[] proc = new String[args.length + 2];
        System.arraycopy(args, 0, proc, 2, args.length);
        proc[0] = "python3";
        proc[1] = scriptFile.toString();
        return new ProcessBuilder(proc).directory(tmpDir).start();
    }
}
