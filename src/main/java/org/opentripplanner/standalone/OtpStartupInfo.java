package org.opentripplanner.standalone;

import org.opentripplanner.common.MavenVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class OtpStartupInfo {
    private static final Logger LOG = LoggerFactory.getLogger(OtpStartupInfo.class);
    private static final String NEW_LINE = "\n";
    public static final List<String> HEADER = List.of(
            "  ___                   _____    _        ___ _   ",
            " / _ \\ _ __  ___ _ _   |_   _| _(_)_ __  | _ \\ |__ _ _ _  _ _  ___ _ _",
            "| (_) | '_ \\/ -_) ' \\    | || '_| | '_ \\ |  _/ / _` | ' \\| ' \\/ -_) '_|",
            " \\___/| .__/\\___|_||_|   |_||_| |_| .__/ |_| |_\\__,_|_||_|_||_\\___|_|",
            "      |_|                         |_|                       "
    );

    private static final String INFO;

    static {
        final int width = HEADER.stream().mapToInt(String::length).max().orElse(0);
        String line = " --" + pad("", '-', width) + "--" + NEW_LINE;
        INFO = line
                + HEADER.stream().map(txt -> line(txt, width)).collect(Collectors.joining())
                + line(" Version:  " + MavenVersion.VERSION.version, width)
                + line(" Commit:   " + MavenVersion.VERSION.commit, width)
                + line(" Branch:   " + MavenVersion.VERSION.branch, width)
                + line(" Build:    " + MavenVersion.VERSION.buildTime, width)
                + dirtyLineIfDirty(width)
                + line;
    }

    private static String dirtyLineIfDirty(int width) {
        return MavenVersion.VERSION.dirty
        ? line(" Dirty:    Local modification exist!", width)
        : "";
    }

    public static void logInfo() {
        LOG.info(NEW_LINE + INFO);
    }

    private static String line(String text, int length) {
        return "|  " + pad(text, ' ', length) + "  |" + NEW_LINE;
    }

    public static String pad(String text, char ch, int length) {
        StringBuilder buf = new StringBuilder(text);
        while (buf.length() < length) { buf.append(ch); }
        return buf.toString();
    }

    public static void main(String[] args) {
        System.out.println(INFO);
    }
}
