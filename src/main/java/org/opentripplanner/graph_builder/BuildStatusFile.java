package org.opentripplanner.graph_builder;

import org.apache.commons.io.IOUtils;
import org.opentripplanner.datastore.CompositeDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;


/**
 * This class create a status file {@code otp-status.inProgress} as soon as the
 * {@link #start(CompositeDataSource, String)} method is invoked. When the Java process exit it will
 * change the file extension to {@code .ok} or {@code .failed}.
 * <p/>
 * This class adds a ShutdownHook ({@link Runtime#addShutdownHook(Thread)}) to be able to update
 * the status file before the process terminate. If the {@link ##exitStatusOk()} is called, the
 * status is set to ok, otherwise it will be failed.
 * <p/>
 * Use static method {@link #start(CompositeDataSource, String)} to write the ".inProgress" file
 * to disk, and call the static method {@link #exitStatusOk()} before main exits. The static method
 * {@link #exitStatusFailed()} is optional, and if called before the {@link #exitStatusOk()} the
 * status is set to failed.
 */
public class BuildStatusFile extends Thread {
    private static final Logger LOG = LoggerFactory.getLogger(BuildStatusFile.class);
    private static final BuildStatusFile INSTANCE = new BuildStatusFile();

    private CompositeDataSource statusFileDir;
    private String ok;
    private String inProgress;
    private String failed;

    private BuildStatusFile() {
        super("OtpStatusFile-shutdown-hook");
    }

    public static void start(CompositeDataSource statusFileDir, String filename) {
        if(filename == null) {
            LOG.warn("The 'otp-status.inProgress' file is skipped, the filename is not configured.");
            return;
        }
        INSTANCE.doStart(statusFileDir, filename);
    }

    /**
     * This will rename the otp-status file and change the file extension to "ok". This method is
     * safe to call, even if the {@link #start(CompositeDataSource, String)} is not called - in that
     * case nothing happens.
     */
    public static void exitStatusOk() {
        INSTANCE.updateStatus(true);
    }

    /**
     * This will rename the otp-status file and change the file extension to "failed". This method
     * is safe to call, even if the {@link #start(CompositeDataSource, String)} is skipped - in that
     * case nothing happens.
     */
    public static void exitStatusFailed() {
        INSTANCE.updateStatus(false);
    }

    @Override
    public void run() {
        exitStatusFailed();
    }

    private void doStart(CompositeDataSource statusFileDir, String filename) {
        INSTANCE.statusFileDir = statusFileDir;
        INSTANCE.inProgress = filename + ".inProgress";
        INSTANCE.ok = filename + ".ok";
        INSTANCE.failed = filename + ".failed";
        setInProgress();
        Runtime.getRuntime().addShutdownHook(this);
    }

    private void setInProgress() {
        try {
            statusFileDir.delete(ok);
            statusFileDir.delete(failed);

            try(OutputStream out = statusFileDir.entry(inProgress).asOutputStream()) {
                IOUtils.write("{}",  out, "UTF-8");
                out.flush();
            }
        } catch (IOException e) {
            // If it fails, do not update status later. Setting the 'statusFileDir' to null
            // prevents updating the status.
            LOG.error(e.getLocalizedMessage(), e);
        }
    }

    private void updateStatus(boolean exitOk) {
        try {
            if(statusFileDir != null) {
                statusFileDir.rename(inProgress, exitOk ? ok : failed);
            }
        }
        catch (Exception e) {
            // This should never happen...
            e.printStackTrace();
        }
        finally {
            // Prevent update from running twice...
            statusFileDir = null;
        }
    }
}
