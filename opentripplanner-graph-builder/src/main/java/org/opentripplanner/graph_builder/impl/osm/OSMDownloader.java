/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;

public class OSMDownloader {
    private static final Logger _log = LoggerFactory.getLogger(OSMDownloader.class);

    private double _latYStep = 0.04;

    private double _lonXStep = 0.04;

    private double _overlap = 0.001;

    private File _cacheDirectory;

    private int _updateIfOlderThanDuration = 0;

    //if this fails, try http://osmxapi.hypercube.telascience.org/api/0.6/
    private String apiBaseUrl = "http://api.openstreetmap.org/api/0.6/";

    public void setLatStep(double latStep) {
        _latYStep = latStep;

    }

    public void setLonStep(double lonStep) {
        _lonXStep = lonStep;
    }

    public void setOverlap(double overlap) {
        _overlap = overlap;
    }

    public void setCacheDirectory(File cacheDirectory) {
        _cacheDirectory = cacheDirectory;
    }

    public void setUpdateIfOlderThanDuration(int durationIndDays) {
        _updateIfOlderThanDuration = durationIndDays;
    }

    public void visitRegion(Envelope rectangle, OSMDownloaderListener listener) throws IOException {

        double minY = floor(rectangle.getMinY(), _latYStep);
        double maxY = ceil(rectangle.getMaxY(), _latYStep);
        double minX = floor(rectangle.getMinX(), _lonXStep);
        double maxX = ceil(rectangle.getMaxX(), _lonXStep);

        for (double y = minY; y < maxY; y += _latYStep) {
            for (double x = minX; x < maxX; x += _lonXStep) {
                String key = getKey(x, y);
                File path = getPathToUpToDateMapTile(y, x, key);
                listener.handleMapTile(key, y, x, path);
            }
        }
    }

    public static double floor(double value, double step) {
        return step * Math.floor(value / step);
    }

    public static double ceil(double value, double step) {
        return step * Math.ceil(value / step);
    }

    private String formatNumberWithoutLocale(double number) {
	return String.format((Locale) null, "%.4f", number);
    }

    private String getKey(double x, double y) {
        return formatNumberWithoutLocale(y) + "_" + formatNumberWithoutLocale(x) + "_" + formatNumberWithoutLocale(_latYStep)
                + "_" + formatNumberWithoutLocale(_lonXStep) + "_" + formatNumberWithoutLocale(_overlap);
    }

    private File getPathToUpToDateMapTile(double lat, double lon, String key) throws IOException {
        
        File path = getPathToMapTile(key);

        if (needsUpdate(path)) {
            Envelope r = new Envelope(lon - _overlap, lon + _lonXStep + _overlap, lat - _overlap,
                    lat + _latYStep + _overlap);

            _log.debug("downloading osm tile: " + key);

            URL url = constructUrl(r);
            _log.debug(url.toString());

            InputStream in = url.openStream();
            FileOutputStream out = new FileOutputStream(path);            
            byte[] data = new byte[4096];
            while (true) {
                int numBytes = in.read(data);
                if (numBytes == -1) {
                    break;
                }
                out.write(data, 0, numBytes);
            }
            in.close();
            out.close();
        }

        return path;
    }

    private File getPathToMapTile(String key) throws IOException {
        if( _cacheDirectory == null) {
            File tmpDir = new File(System.getProperty("java.io.tmpdir"));
            _cacheDirectory = new File(tmpDir,"osm-tiles");
        }
        
        if (!_cacheDirectory.exists()) {
            if (!_cacheDirectory.mkdirs()) {
                throw new RuntimeException("Failed to create directory " + _cacheDirectory);
            }
        }

        File path = new File(_cacheDirectory, "map-" + key + ".osm");
        return path;
    }

    private boolean needsUpdate(File path) {
        if (!path.exists())
            return true;
        if (_updateIfOlderThanDuration > 0) {
            if (System.currentTimeMillis() - path.lastModified() > _updateIfOlderThanDuration)
                return true;
        }
        return false;
    }

    private URL constructUrl(Envelope r) {
        double left = r.getMinX();
        double right = r.getMaxX();
        double bottom = r.getMinY();
        double top = r.getMaxY();
        try {
            return new URL(getApiBaseUrl() + "map?bbox=" + left + "," + bottom
                    + "," + right + "," + top);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 
     * Set the base OSM API URL from which OSM tiles will be downloaded.    
     */
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }
}
