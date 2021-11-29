package org.opentripplanner.standalone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.opentripplanner.model.projectinfo.OtpProjectInfo.projectInfo;

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
            + line("Version:    " + projectInfo().version.version)
            + line("Ser.ver.id: " + projectInfo().getOtpSerializationVersionId())
            + line("Commit:     " + projectInfo().versionControl.commit)
            + line("Branch:     " + projectInfo().versionControl.branch)
            + line("Build:      " + projectInfo().versionControl.buildTime)
            + dirtyLineIfDirty();
    }

    private static String dirtyLineIfDirty() {
        return projectInfo().versionControl.dirty
        ? line("Dirty:    Local modification exist!")
        : "";
    }

    public static void logInfo() {
        // This is good when aggregating logs across multiple load balanced instances of OTP
        // Hint: a reg-exp filter like "^OTP (START|SHUTTING)" will list nodes going up/down
        LOG.info("OTP STARTING UP (" + projectInfo().getVersionString() + ")");
        Runtime.getRuntime().addShutdownHook(new Thread(() ->
            LOG.info("OTP SHUTTING DOWN (" + projectInfo().getVersionString() + ")"))
        );
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
