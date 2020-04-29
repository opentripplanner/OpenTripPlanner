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
             "  ___                 _____     _       ____  _                             ",
             " / _ \\ _ __   ___ _ _|_   _| __(_)_ __ |  _ \\| | __ _ _ __  _ __   ___ _ __ ",
             "| | | | '_ \\ / _ \\ '_ \\| || '__| | '_ \\| |_) | |/ _` | '_ \\| '_ \\ / _ \\ '__|",
             "| |_| | |_) |  __/ | | | || |  | | |_) |  __/| | (_| | | | | | | |  __/ |   ",
             " \\___/| .__/ \\___|_| |_|_||_|  |_| .__/|_|   |_|\\__,_|_| |_|_| |_|\\___|_| ",
             "      |_|                        |_| "
    );

    private static final String INFO;

    static {
        INFO = ""
                + HEADER.stream().map(OtpStartupInfo::line).collect(Collectors.joining())
                + line("Version:  " + MavenVersion.VERSION.version)
                + line("Commit:   " + MavenVersion.VERSION.commit)
                + line("Branch:   " + MavenVersion.VERSION.branch)
                + line("Build:    " + MavenVersion.VERSION.buildTime)
                + dirtyLineIfDirty();
    }

    private static String dirtyLineIfDirty() {
        return MavenVersion.VERSION.dirty
        ? line("Dirty:    Local modification exist!")
        : "";
    }

    public static void logInfo() {
        LOG.info(NEW_LINE + INFO);
    }

    private static String line(String text) {
        return text + NEW_LINE;
    }

    /** Use this to do a manual test */
    public static void main(String[] args) {
        System.out.println(INFO);
    }
}
