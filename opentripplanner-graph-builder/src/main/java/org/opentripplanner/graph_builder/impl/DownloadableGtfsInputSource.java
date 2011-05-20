package org.opentripplanner.graph_builder.impl;

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

    private static final Logger _log = LoggerFactory.getLogger(DownloadableGtfsInputSource.class);

    private URL _url;
   
    private File _cacheDirectory;
    
    private String _defaultAgencyId;
    
    // Pattern: Decorator
    private ZipFileCsvInputSource _zip;  

    public void setUrl(URL url) {
        _url = url;
    }
  
    public void setCacheDirectory(File cacheDirectory) {
        _cacheDirectory = cacheDirectory;
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

        if (_cacheDirectory != null) {
            if (!_cacheDirectory.exists())
                _cacheDirectory.mkdirs();
            return _cacheDirectory;
        }

        return new File(System.getProperty("java.io.tmpdir"));
    }

    private File getPathForGtfsBundle() throws IOException {

        if (_url != null) {

            File tmpDir = getTemporaryDirectory();
            String fileName = _defaultAgencyId + "_gtfs.zip";
            File gtfsFile = new File(tmpDir, fileName);

            if (gtfsFile.exists()) {
                _log.info("using already downloaded gtfs file: path=" + gtfsFile);
                return gtfsFile;
            }

            _log.info("downloading gtfs: url=" + _url + " path=" + gtfsFile);

            BufferedInputStream in = new BufferedInputStream(_url.openStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(gtfsFile));

            copyStreams(in, out);

            return gtfsFile;
        }

        throw new IllegalStateException("DownloadableGtfsInputSource did not include an url");
    }
    
    private synchronized void checkIfDownloaded() throws IOException {
        if (_zip == null) {
            _zip = new ZipFileCsvInputSource(new ZipFile(getPathForGtfsBundle()));
        }
    }
    
	@Override
	public boolean hasResource(String name) throws IOException {
        checkIfDownloaded();
        return _zip.hasResource(name);
	}

	@Override
	public InputStream getResource(String name) throws IOException {
        checkIfDownloaded();
        return _zip.getResource(name);
	}

	@Override
	public void close() throws IOException {
        checkIfDownloaded();
        _zip.close();
	}
}
