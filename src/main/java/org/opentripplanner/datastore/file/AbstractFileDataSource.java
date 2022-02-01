package org.opentripplanner.datastore.file;

import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;

import java.io.File;
import java.util.Objects;

/**
 * Abstract DataSource which wraps a {@link File}.
 */
public abstract class AbstractFileDataSource implements DataSource {

    final File file;
    final FileType type;

    /**
     * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
     * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
     * zip-files {@link ZipFileDataSource} which contain multiple files.
     */
    AbstractFileDataSource(File file, FileType type) {
        this.file = file;
        this.type = type;
    }

    @Override
    public final String name() {
        return file.getName();
    }

    @Override
    public final String path() {
        return file.getPath();
    }

    @Override
    public final FileType type() {
        return type;
    }

    @Override
    public final long size() {
        return file.length();
    }

    @Override
    public final long lastModified() {
        return file.lastModified();
    }

    @Override
    public final boolean exists() {
        return file.exists() && file.canRead();
    }

    @Override
    public boolean isWritable() {
        // We assume we can write to a file if the parent directory exist, and if the
        // file it self exist then it must be writable. If the file do not exist
        // we assume we can create a new file and write to it - there is no check on this.
        return file.getParentFile().exists() && (!file.exists() || file.canWrite());
    }

    @Override
    public String toString() {
        return type + " " + path();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) { return true; }
        if (o == null || getClass() != o.getClass()) { return false; }
        AbstractFileDataSource that = (AbstractFileDataSource) o;
        return file.equals(that.file) && type == that.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(file, type);
    }
}
