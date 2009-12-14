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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.NumberFormat;

import com.vividsolutions.jts.geom.Envelope;

public class OSMDownloader {

    private NumberFormat _format = new DecimalFormat("0.0000");

    private double _latYStep = 0.04;

    private double _lonXStep = 0.04;

    private double _overlap = 0.001;

    private File _cacheDirectory;

    private int _updateIfOlderThanDuration = 0;

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

    private double floor(double value, double step) {
        return step * Math.floor(value / step);
    }

    private double ceil(double value, double step) {
        return step * Math.ceil(value / step);
    }

    private String getKey(double x, double y) {
        return _format.format(y) + "_" + _format.format(x) + "_" + _format.format(_latYStep)
                + "_" + _format.format(_lonXStep) + "_" + _format.format(_overlap);
    }

    private File getPathToUpToDateMapTile(double lat, double lon, String key) throws IOException {
        
        File path = getPathToMapTile(key);

        if (needsUpdate(path)) {
            Envelope r = new Envelope(lon - _overlap, lon + _lonXStep + _overlap, lat - _overlap,
                    lat + _latYStep + _overlap);

            System.out.println("downloading osm tile: " + key);

            URL url = constructUrl(r);
            System.out.println(url);
            String value = getUrlAsString(url);

            PrintWriter writer = new PrintWriter(new FileWriter(path));
            writer.println(value);
            writer.close();
        }

        return path;
    }

    private File getPathToMapTile(String key) throws IOException {
        if( _cacheDirectory == null) {
            _cacheDirectory = File.createTempFile("OpenStreetMapDownloader-", "-tmp");
            _cacheDirectory.delete();
        }
        
        _cacheDirectory.mkdirs();

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

    private String getUrlAsString(URL url) throws IOException {
        InputStream in = url.openStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
        String line = null;
        StringBuilder b = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            b.append(line);
            b.append("\n");
        }
        reader.close();
        return b.toString();
    }

    private URL constructUrl(Envelope r) {
        double left = r.getMinX();
        double right = r.getMaxX();
        double bottom = r.getMinY();
        double top = r.getMaxY();
        try {
            return new URL("http://api.openstreetmap.org/api/0.6/map?bbox=" + left + "," + bottom
                    + "," + right + "," + top);
        } catch (MalformedURLException e) {
            throw new IllegalStateException(e);
        }
    }
}
