package org.opentripplanner.graph_builder.module;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.zip.ZipFile;

import org.onebusaway.csv_entities.CsvInputSource;
import org.onebusaway.csv_entities.ZipFileCsvInputSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DownloadableGtfsInputSource implements CsvInputSource {

    private static final Logger LOG = LoggerFactory.getLogger(DownloadableGtfsInputSource.class);

    private URL url;

    private File cacheDirectory;

    private String defaultAgencyId;
    
    public boolean useCached = true;

    // Pattern: Decorator
    private ZipFileCsvInputSource zip;

    public void setUrl(URL url) {
        this.url = url;
    }

    public void setCacheDirectory(File cacheDirectory) {
        this.cacheDirectory = cacheDirectory;
    }

    private void copyStreams(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        while (true) {
            int rc = in.read(buffer);
            if (rc == -1)
                break;
            out.write(buffer, 0, rc);
        }
        in.close();
        out.close();
    }

    private File getTemporaryDirectory() {

        if (cacheDirectory != null) {
            if (!cacheDirectory.exists()) {
                if (!cacheDirectory.mkdirs()) {
                    throw new RuntimeException("Failed to create cache directory " + cacheDirectory);
                }
            }
            return cacheDirectory;
        }

        return new File(System.getProperty("java.io.tmpdir"));
    }

    private File getPathForGtfsBundle() throws IOException {

        if (url != null) {

            File tmpDir = getTemporaryDirectory();
            String cacheFile = defaultAgencyId;
            if (cacheFile == null) {
                // Build a cache file based on URL
                cacheFile = (url.getHost() + url.getFile()).replace("/", "_");
            }
            String fileName = cacheFile + "_gtfs.zip";
            File gtfsFile = new File(tmpDir, fileName);

            if (gtfsFile.exists()) {
                if (useCached) {
                    LOG.info("using already downloaded gtfs file: path=" + gtfsFile);
                    return gtfsFile;
                }
                LOG.info("useCached=false; GTFS will be re-downloaded." + gtfsFile);
            }

            LOG.info("downloading gtfs: url=" + url + " path=" + gtfsFile);

            BufferedInputStream in = new BufferedInputStream(url.openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(gtfsFile));
            try {
                copyStreams(in, out);
            } catch (RuntimeException e) {
                out.close();
                if (!gtfsFile.delete()) {
                    LOG.error("Failed to delete incomplete file " + gtfsFile);
                }
                throw e;
            }
            return gtfsFile;
        }

        throw new IllegalStateException("DownloadableGtfsInputSource did not include an url");
    }

    private synchronized void checkIfDownloaded() throws IOException {
        if (zip == null) {
            zip = new ZipFileCsvInputSource(new ZipFile(getPathForGtfsBundle()));
        }
    }

    @Override
    public boolean hasResource(String name) throws IOException {
        checkIfDownloaded();
        return zip.hasResource(name);
    }

    @Override
    public InputStream getResource(String name) throws IOException {
        checkIfDownloaded();
        return zip.getResource(name);
    }

    @Override
    public void close() throws IOException {
        checkIfDownloaded();
        zip.close();
    }
}
