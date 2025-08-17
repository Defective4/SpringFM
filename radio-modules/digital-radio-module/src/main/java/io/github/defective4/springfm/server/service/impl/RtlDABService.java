package io.github.defective4.springfm.server.service.impl;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import javax.sound.sampled.AudioFormat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.github.defective4.springfm.server.packet.DataGenerator;
import io.github.defective4.springfm.server.service.DigitalRadioService;
import io.github.defective4.springfm.server.service.ServiceArgument;
import io.github.defective4.springfm.server.util.DependencyUtils;
import io.github.defective4.springfm.server.util.ScriptUtils;
import io.github.defective4.springfm.server.util.ThreadUtils;

public class RtlDABService implements DigitalRadioService {

    public static class DABStation {
        private final String name, id;

        public DABStation(String name, String id) {
            this.name = name;
            this.id = id;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    public static class Ensemble {
        private final String channel, ensemble;
        private final Map<String, String> stations;

        public Ensemble(String channel, String ensemble, Map<String, String> stations) {
            this.channel = channel;
            this.ensemble = ensemble;
            this.stations = stations;
        }

        public String getChannel() {
            return channel;
        }

        public String getEnsemble() {
            return ensemble;
        }

        public Map<String, String> getStations() {
            return stations;
        }
    }

    private final String channel;

    private Future<?> dablinAudioTask;
    private final Object dablinEtiLock = new Object();
    private InputStream dablinInput;
    private final Object dablinLock = new Object();
    private OutputStream dablinOutput;
    private final String dablinPath;

    private Process dablinProcess;

    private boolean debug;

    private final String etiCmdlinePath;

    private InputStream etiInput;

    private Process etiProcess;

    private Future<?> etiTask;

    private final AudioFormat format;
    private final AudioFormat format2;

    private final int gain;

    private DataGenerator generator;

    private final List<String> ignoredServices;

    private final String name;

    private DataInputStream soxInput;

    private OutputStream soxOutput;

    private final String soxPath;

    private Process soxProcess;

    private Future<?> soxTask;

    private int stationIndex;
    private final List<DABStation> stations = new ArrayList<>();

    public RtlDABService(@ServiceArgument(name = "name", defaultValue = "DAB") String name,
            @ServiceArgument(name = "etiCmdlinePath", defaultValue = "eti-cmdline-rtlsdr") String etiCmdlinePath,
            @ServiceArgument(name = "channel") String channel,
            @ServiceArgument(name = "dablinPath", defaultValue = "dablin") String dablinPath,
            @ServiceArgument(name = "soxPath", defaultValue = "sox") String soxPath,
            @ServiceArgument(name = "gain", defaultValue = "auto") String gain,
            @ServiceArgument(name = "ignoredServices") JsonArray ignoredServices, AudioFormat format) {
        format2 = format;
        if (format.getChannels() != 2) throw new IllegalArgumentException("Only stereo audio format is allowed");
        int gainVal;
        if (gain.equalsIgnoreCase("auto")) {
            gainVal = -1;
        } else
            try {
                gainVal = Integer.parseInt(gain);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid gain value. It must either be an integer, or \"auto\"");
            }

        if (channel == null)
            throw new IllegalArgumentException("You must specify a DAB channel in service configuration");

        this.gain = gainVal;
        this.etiCmdlinePath = etiCmdlinePath;
        this.dablinPath = dablinPath;
        this.soxPath = soxPath;
        this.channel = channel;
        this.format = format;
        this.name = name;

        List<String> ignoredSvcList = new ArrayList<>();
        for (JsonElement el : ignoredServices) ignoredSvcList.add(el.getAsString());
        this.ignoredServices = Collections.unmodifiableList(ignoredSvcList);
    }

    @Override
    public Collection<String> checkMissingDependencies() {
        List<String> dependencies = new ArrayList<>();
        if (!DependencyUtils.checkHelpableCommand(etiCmdlinePath, 2)) dependencies.add("eti-cmdline-rtlsdr");
        if (!DependencyUtils.checkHelpableCommand(dablinPath, 1)) dependencies.add("dablin");
        if (!DependencyUtils.checkHelpableCommand(soxPath)) dependencies.add("sox");
        return Collections.unmodifiableCollection(dependencies);
    }

    @Override
    public AudioFormat getAudioFormat() {
        return format;
    }

    @Override
    public int getCurrentStation() {
        return stationIndex;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String[] getStations() {
        return stations.stream().map(e -> e.getName()).toList().toArray(new String[0]);
    }

    @Override
    public void init() throws IOException {
        try {
            System.err.println("Trying to receive DAB ensemble on channel 11A...");
            File tempDir = Files.createTempDirectory("springfmdab").toFile();
            File ensembleFile = new File(tempDir, "ensemble-ch-" + channel + ".json");
            ScriptUtils.startProcess(etiCmdlinePath, tempDir, "-J", "-x", "-C", channel).waitFor();
            if (!ensembleFile.isFile()) throw new IOException("Couldn't receive DAB ensemble (No signal)");
            StringBuilder jsonBuilder = new StringBuilder();
            try (Reader reader = new InputStreamReader(Files.newInputStream(ensembleFile.toPath()),
                    StandardCharsets.UTF_8)) {
                while (true) {
                    int c = reader.read();
                    if (c < 0) break;
                    jsonBuilder.append((char) c);
                }
            }
            ScriptUtils.recursiveDelete(tempDir);
            String json = jsonBuilder.toString().replace("\"Eid:\"", "\"Eid\":\"");
            Ensemble ensemble = new Gson().fromJson(json, Ensemble.class);
            for (Map.Entry<String, String> station : ensemble.getStations().entrySet()) {
                if (!ignoredServices.contains(station.getKey()))
                    stations.add(new DABStation(station.getKey(), station.getValue()));
            }
            System.err.println(
                    "Received ensemble \"" + ensemble.getEnsemble() + "\" with " + stations.size() + " stations.");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isStarted() {
        return etiProcess != null && etiProcess.isAlive() && dablinProcess != null && dablinProcess.isAlive()
                && soxProcess != null && soxProcess.isAlive();
    }

    @Override
    public void setDebugMode(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void setPacketGenerator(DataGenerator generator) {
        this.generator = generator;
    }

    @Override
    public void start() throws IOException {
        if (isStarted()) throw new IllegalStateException("Already started");
        stop();
        Object[] etiArgs = new String[] { "-C", channel, "-Q" };
        if (gain >= 0) {
            etiArgs = Arrays.copyOf(etiArgs, etiArgs.length + 1);
            etiArgs[etiArgs.length - 2] = "-G";
            etiArgs[etiArgs.length - 1] = gain;
        }
        etiProcess = ScriptUtils.startProcess(etiCmdlinePath, etiArgs);
        tuneDablin(stationIndex);
        soxProcess = ScriptUtils.startProcess(soxPath, "-t", "raw", "-r", "48k", "-c", "2", "-ef", "-b", "32", "-",
                "-t", "raw", "-r", Integer.toString((int) format.getFrameRate()), "-c", "2", "-es", "-b", "16", "-");
        soxOutput = soxProcess.getOutputStream();
        soxInput = new DataInputStream(soxProcess.getInputStream());
        etiInput = etiProcess.getInputStream();

        etiTask = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[1024];
                while (isStarted()) {
                    synchronized (dablinEtiLock) {
                        int read = etiInput.read(buffer);
                        if (read < 0) break;
                        dablinOutput.write(buffer, 0, read);
                        dablinOutput.flush();
                    }
                }
            } catch (Exception e) {
                if (debug) e.printStackTrace();
                try {
                    stop();
                } catch (IOException e1) {}
            }
        });

        dablinAudioTask = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096 * 2];
                while (isStarted()) {
                    synchronized (dablinLock) {
                        int read = dablinInput.read(buffer);
                        if (read < 0) break;
                        soxOutput.write(buffer, 0, read);
                    }
                }
            } catch (Exception e) {
                if (debug) e.printStackTrace();
                try {
                    stop();
                } catch (IOException e1) {}
            }
        });

        soxTask = ThreadUtils.submit(() -> {
            try {
                byte[] buffer = new byte[4096];
                while (isStarted()) {
                    soxInput.readFully(buffer);
                    generator.audioSampleGenerated(buffer, false);
                }
            } catch (Exception e) {
                if (debug) e.printStackTrace();
                try {
                    stop();
                } catch (IOException e1) {}
            }
        });
    }

    @Override
    public void stop() throws IOException {
        try {
            if (etiProcess != null) etiProcess.destroyForcibly();
            if (dablinProcess != null) dablinProcess.destroyForcibly();
            if (soxProcess != null) soxProcess.destroyForcibly();

            if (etiTask != null) etiTask.cancel(true);
            if (dablinAudioTask != null) dablinAudioTask.cancel(true);
            if (soxTask != null) soxTask.cancel(true);

            if (etiInput != null) etiInput.close();
            if (dablinInput != null) dablinInput.close();
            if (dablinOutput != null) dablinOutput.close();
            if (soxInput != null) soxInput.close();
            if (soxOutput != null) soxOutput.close();
        } finally {
            etiProcess = null;
            dablinProcess = null;
            soxProcess = null;

            etiTask = null;
            dablinAudioTask = null;
            soxTask = null;
        }
    }

    @Override
    public void tune(int stationIndex) throws IllegalArgumentException, IOException {
        tuneDablin(stationIndex);
        this.stationIndex = stationIndex;
    }

    private void tuneDablin(int index) throws IOException {
        if (index < 0 || index >= stations.size()) throw new IllegalArgumentException("Station index out of bounds");
        synchronized (dablinLock) {
            synchronized (dablinEtiLock) {
                try {
                    if (dablinProcess != null) dablinProcess.destroyForcibly();
                    if (dablinInput != null) dablinInput.close();
                    if (dablinOutput != null) dablinOutput.close();
                } catch (Exception e) {}
                dablinProcess = ScriptUtils.startProcess(dablinPath, "-f", "eti", "-p", "-s",
                        stations.get(index).getId());
                dablinOutput = dablinProcess.getOutputStream();
                dablinInput = dablinProcess.getInputStream();
            }
        }
    }

}
