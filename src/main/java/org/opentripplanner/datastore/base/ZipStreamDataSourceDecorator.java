package org.opentripplanner.datastore.base;

import org.opentripplanner.datastore.CompositeDataSource;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;


/**
 * This decorator help unzip the content of any underling data source(the delegate).
 * This make it easier to provide a store implementation - since this code can be reused.
 * <p/>
 * See the Google Cloud Store implementation for an example on hwo to use it.
 */
public class ZipStreamDataSourceDecorator implements CompositeDataSource {


    private final DataSource delegate;

    /**
     * This store load the zip file into memory; hence we should load the content only once,
     * even at the risk that the source is changed since last tile a resource is accessed.
     * To achieve this we use a boolean flag to indicate if the content is loaded or not.
     */
    private boolean contentLoaded = false;

    /**
     * Cache content from first access to this data source is closed.
     */
    private List<DataSource> content = new ArrayList<>();

    /**
     * Create a Zip Stream data source decorator around another data source. The given delegate
     * is responsible for retrieving meta-data and providing an input stream to fetch the
     * zipped content.
     */
    public ZipStreamDataSourceDecorator(DataSource delegate) {
        this.delegate = delegate;
    }

    @Override
    public String name() {
        return delegate.name();
    }

    @Override
    public String path() {
        return delegate.path();
    }

    @Override
    public FileType type() {
        return delegate.type();
    }

    @Override
    public long size() {
        return delegate.size();
    }

    @Override
    public long lastModified() {
        return delegate.lastModified();
    }

    @Override
    public boolean exists() {
        return delegate.exists();
    }

    @Override
    public String detailedInfo() {
        return delegate.detailedInfo();
    }

    @Override
    public boolean isWritable() {
        return false;
    }

    @Override
    public Collection<DataSource> content() {
        loadContent();
        return content;
    }

    @Override
    public DataSource entry(String name) {
        loadContent();
        return content.stream().filter(it -> name.equals(it.name())).findFirst().orElse(null);
    }

    @Override
    public InputStream asInputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                        + " do not support READING. Can not read from: " + path()
        );
    }

    @Override
    public OutputStream asOutputStream() {
        throw new UnsupportedOperationException(
                "This datasource type " + type()
                        + " do not support WRITING. Can not write to: " + path()
        );
    }

    @Override
    public void close() {
        // Make the content available for GC.
        content = null;
    }

    @Override
    public String toString() {
        return path();
    }

    private void loadContent() {
        if(content == null) {
            throw new NullPointerException(
                    "The content is accessed after the zip file is closed: " + path()
            );
        }

        if(contentLoaded) { return; }
        contentLoaded = true;

        // We support both gzip and unzipped files when reading.
        try(
                ZipInputStream zis = new ZipInputStream(delegate.asInputStream())
        ) {
            for (ZipEntry entry = zis.getNextEntry(); entry != null; entry = zis.getNextEntry()) {
                // we only support flat ZIP files
                if (entry.isDirectory()) { continue; }

                ByteArrayOutputStream buf = new ByteArrayOutputStream(4048);
                zis.transferTo(buf);
                byte[] bArray = buf.toByteArray();

                content.add(
                        new ByteArrayDataSource(
                            entry.getName() + " (" + path() + ")",
                            entry.getName(),
                            type(),
                            bArray.length,
                            entry.getLastModifiedTime().toMillis(),
                            false
                        ).withBytes(bArray)
                );
            }
        } catch (ZipException ex) {
            throw new RuntimeException(
                    "Can not read zip file " + path() + ": " + ex.getLocalizedMessage(),
                    ex
            );
        }
        catch (IOException ie) {
            throw new RuntimeException(
                    "Failed to load " + path() + ": " + ie.getLocalizedMessage(),
                    ie
            );
        }
    }
}
