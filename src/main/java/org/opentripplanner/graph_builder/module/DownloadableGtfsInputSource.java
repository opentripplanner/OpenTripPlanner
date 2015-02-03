/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (props, at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

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

    private URL _url;

    private File _cacheDirectory;

    private String _defaultAgencyId;
    
    public boolean useCached = true;

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
            if (!_cacheDirectory.exists()) {
                if (!_cacheDirectory.mkdirs()) {
                    throw new RuntimeException("Failed to create cache directory " + _cacheDirectory);
                }
            }
            return _cacheDirectory;
        }

        return new File(System.getProperty("java.io.tmpdir"));
    }

    private File getPathForGtfsBundle() throws IOException {

        if (_url != null) {

            File tmpDir = getTemporaryDirectory();
            String cacheFile = _defaultAgencyId;
            if (cacheFile == null) {
                // Build a cache file based on URL
                cacheFile = (_url.getHost() + _url.getFile()).replace("/", "_");
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

            LOG.info("downloading gtfs: url=" + _url + " path=" + gtfsFile);

            BufferedInputStream in = new BufferedInputStream(_url.openStream());
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
