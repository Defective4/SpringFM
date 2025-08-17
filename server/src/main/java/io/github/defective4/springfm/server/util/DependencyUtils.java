package io.github.defective4.springfm.server.util;

import java.io.DataInputStream;

public class DependencyUtils {
    private DependencyUtils() {
    }

    public static boolean checkGnuRadio() {
        try {
            return ScriptUtils.runGnuRadioScript("gr_check").waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkGnuRadioRDS() {
        try {
            return ScriptUtils.runGnuRadioScript("gr_rds_check").waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkHelpableCommand(String path) {
        return checkHelpableCommand(path, 0);
    }

    public static boolean checkHelpableCommand(String path, int expectedCode) {
        try {
            return ScriptUtils.startProcess(path, "-h").waitFor() == expectedCode;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkPython3() {
        try {
            return ScriptUtils.startProcess("python3", "--version").waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkRedsea() {
        try {
            return ScriptUtils.startProcess("redsea", "-v").waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean checkRtlFm(String path) {
        try {
            Process rtlFm = ScriptUtils.startProcess(path, "-f", "88M", "-");
            byte[] buffer = new byte[1024];
            try (DataInputStream in = new DataInputStream(rtlFm.getInputStream())) {
                in.readFully(buffer);
            }
            rtlFm.destroyForcibly();
            return rtlFm.waitFor() != 0;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
