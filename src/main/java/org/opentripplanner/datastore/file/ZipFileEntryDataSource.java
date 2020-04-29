package org.opentripplanner.datastore.file;

import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;

public class ZipFileEntryDataSource implements DataSource {
    private final ZipFileDataSource dataSource;
    private final ZipEntry entry;

    ZipFileEntryDataSource(ZipFileDataSource dataSource, ZipEntry entry) {
        this.dataSource = dataSource;
        this.entry = entry;
    }

    @Override
    public String name() {
        return entry.getName();
    }

    @Override
    public String path() {
        return name() + " (" + dataSource.path() + ")";
    }

    @Override
    public FileType type() {
        return dataSource.type();
    }

    @Override
    public long size() {
        return entry.getSize();
    }

    @Override
    public long lastModified() {
        return entry.getLastModifiedTime().toMillis();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public InputStream asInputStream() {
        try {
            return dataSource.zipFile().getInputStream(entry);
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Failed to read " + path() + ": " + e.getLocalizedMessage(),
                    e
            );
        }
    }

    @Override
    public String toString() {
        return type() + " " + path();
    }
}
