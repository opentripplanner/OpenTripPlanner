package org.opentripplanner.datastore.file;

import org.opentripplanner.datastore.FileType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;

public class FileDataSource extends AbstractFileDataSource {

    /**
     * Create a data source wrapper around a file. This wrapper handles GZIP(.gz) compressed files
     * as well as normal files. It does not handle directories({@link DirectoryDataSource}) or
     * zip-files {@link ZipFileDataSource} witch contain multiple files.
     */
    public FileDataSource(File file, FileType type) {
        super(file, type);
    }

    @Override
    public InputStream asInputStream() {
        try {
            // We support both gzip and unzipped files when reading.
            if (file.getName().endsWith(".gz")) {
                return new GZIPInputStream(new FileInputStream(file));
            }
            else {
                return new FileInputStream(file);
            }
        }
        catch (IOException e) {
            throw new RuntimeException(
                    "Failed to load " + path() + ": " + e.getLocalizedMessage(),
                    e
            );
        }
    }

    @Override
    public OutputStream asOutputStream() {
        try {
            return new FileOutputStream(file);
        }
        catch (FileNotFoundException e) {
            throw new RuntimeException(
                    "File not found " + path() + ": " + e.getLocalizedMessage(),
                    e
            );
        }
    }
}
