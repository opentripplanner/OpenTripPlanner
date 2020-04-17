package org.opentripplanner.datastore;

import org.apache.commons.io.IOUtils;
import org.opentripplanner.common.LoggingUtil;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;


/**
 * A data source is generalized type to represent an file, database blob or unit that OTP read or
 * write to.
 * <p>
 * The data source instance contain metadata like {@code name}, {@code description}, {@code type}
 * and so on. To access (read from or write to) a datasource the methods {@link #asInputStream()}
 * and {@link #asOutputStream()} will open a connection to the underlying data source and make it
 * available for reading/writing.
 * <p>
 * Only metadata is retrieved before a stream is opened, making sure minimum data is transferred
 * before it is actually needed.
 * <p>
 * The data source metadata should be fetched once. The data is NOT updated even if the source
 * itself changes. If this happens it might cause the streaming to fail.
 * <p>
 * Concurrent modifications to underlying data-sources is not accounted for, and there is no need
 * to support that in the implementation of this class. This means that we assume all input- and
 * output-files in OTP are stable (not changed in any way) during the period OTP need to access
 * these files.
 */
public interface DataSource {

    /**
     * @return the short name identifying the source within its scope (withing a {@link
     * OtpDataStore} or {@link CompositeDataSource}) Including the file extension.
     * <p>
     * Examples:
     * <p>
     * {@code build-config.json, gtfs.zip and stops.txt}
     */
    String name();

    /**
     * @return the full path (or description) to be used when describing this data source. This
     * method is mainly used for humans to identify the source in logs and error handling.
     */
    String path();

    /**
     * The file type this data source is identified as.
     */
    FileType type();

    /**
     * @return size in bytes, if unknown returns {@code -1}
     */
    default long size() { return -1; }

    /**
     * @return last modified timestamp in ms, if unknown returns {@code -1}
     */
    default long lastModified() { return -1; }

    /**
     * @return true is it exist in the data store; hence calling {@link #asInputStream()} is safe.
     */
    default boolean exists() { return true; }

    /**
     * @return {@code true} if it is possible to write to data source. Also, return {@code true} if
     * if it is not easy to check. No guarantee is given and the {@link #asOutputStream()} may
     * fail. This method can be used to avoid consuming a lot of resource before writing to a
     * datasource, if this method return {@code false}.
     */
    default boolean isWritable() { return true; }

    /**
     * Connect to this data source and make it available as an input stream. The caller is
     * responsible to close the connection.
     * <p>
     * Note! This method might get called several times, and each time a new Stream should be
     * created.
     */
    default InputStream asInputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + getClass().getSimpleName()
                        + " do not support READING. Can not read from: " + path()
        );
    }

    /**
     * Return the content as a byte array. The implementation may chose to implement this in a
     * more efficient way - not reading the input stream. Do not change the data returned.
     * <p/>
     * Calling this method is the same as reading everything off the {@link #asInputStream()}.
     */
    default byte[] asBytes() {
        try {
            return IOUtils.toByteArray(asInputStream());
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read " + path() + ": " + e.getLocalizedMessage(),
                    e
            );
        }
    }

    default OutputStream asOutputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + getClass().getSimpleName()
                + " do not support WRITING. Can not write to: " + path()
        );
    }

    /**
     * Return an info string like this:
     * <p>
     * {@code [icon] [filename]  [path]  [date time]  [file size]}
     */
    default String detailedInfo() {
        String dir = path().substring(0, path().length() - name().length() - 1);
        String info = String.format("%s %s  %s", type().icon(), name(), dir);
        if (lastModified() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            info += "  " + sdf.format(lastModified());
        }
        if (size() > 0) {
            info += "  " + LoggingUtil.fileSizeToString(size());
        }
        return info;
    }
}
