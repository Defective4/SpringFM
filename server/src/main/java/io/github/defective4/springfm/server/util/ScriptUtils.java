package io.github.defective4.springfm.server.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Set;

public class ScriptUtils {
    private ScriptUtils() {
    }

    public static Process runGnuRadioScript(String resource, Set<String> dependencies, String... args)
            throws IOException {
        File tmpDir = Files.createTempDirectory("springfm").toFile();

        String[] toCopy = new String[dependencies.size() + 1];
        int index = 1;
        for (String dep : dependencies) toCopy[index++] = dep;
        toCopy[0] = resource;

        for (String element : toCopy)
            try (InputStream in = ScriptUtils.class.getResourceAsStream("/" + element + ".py")) {
                File scriptFile = new File(tmpDir, element + ".py");
                Files.copy(in, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        String[] proc = new String[args.length + 2];
        System.arraycopy(args, 0, proc, 2, args.length);
        proc[0] = "python3";
        proc[1] = resource + ".py";
        Process ps = new ProcessBuilder(proc).directory(tmpDir).start();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ps.destroyForcibly();
            if (tmpDir.isDirectory()) recursiveDelete(tmpDir);
        }));
        return ps;
    }

    public static Process runGnuRadioScript(String resource, String... args) throws IOException {
        return runGnuRadioScript(resource, Collections.emptySet(), args);
    }

    private static void recursiveDelete(File dir) {
        if (dir.isDirectory()) {
            for (File f : dir.listFiles()) recursiveDelete(f);
        }
        dir.delete();
    }
}
