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

package org.opentripplanner.openstreetmap.impl;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.openstreetmap.services.RegionsSource;
import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.vividsolutions.jts.geom.Envelope;

public class RegionBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private static Logger _log = LoggerFactory.getLogger(RegionBasedOpenStreetMapProviderImpl.class);

    private RegionsSource _regionsSource;

    private File _cacheDirectory;

    private String _apiBaseUrl;

    private OSMDownloader downloader = new OSMDownloader();

    public void setRegionsSource(RegionsSource regionsSource) {
        _regionsSource = regionsSource;
    }

    public void setCacheDirectory(File cacheDirectory) {
        _cacheDirectory = cacheDirectory;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {

        if( _cacheDirectory != null)
            downloader.setCacheDirectory(_cacheDirectory);

        if( _apiBaseUrl != null)
            downloader.setApiBaseUrl(_apiBaseUrl);

        DownloadHandler downloadHandler = new DownloadHandler(handler);

        try {
            int regionIndex = 0;
            for (Envelope region : _regionsSource.getRegions()) {
                getDownloader().visitRegion(region, downloadHandler);
                if (regionIndex % 1000 == 0) {
                    _log.debug("regions=" + regionIndex);
                }
                regionIndex++;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("error downloading osm", ex);
        }
    }

    /**
     * Set a custom OSM downloader 
     * @param downloader
     */
    public void setDownloader(OSMDownloader downloader) {
        this.downloader = downloader;
    }

    public OSMDownloader getDownloader() {
        return downloader;
    }

    private static class DownloadHandler implements OSMDownloaderListener {

        private Set<String> _visitedMapTiles = new HashSet<String>();

        private OpenStreetMapParser _parser = new OpenStreetMapParser();

        private OpenStreetMapContentHandler _contentHandler;

        public DownloadHandler(OpenStreetMapContentHandler contentHandler) {
            _contentHandler = contentHandler;
        }

        @Override
        public void handleMapTile(String key, double lat, double lon, File pathToMapTile) {
            try {
                if( _visitedMapTiles.add(key))
                    _parser.parseMap(pathToMapTile, _contentHandler);
            } catch (IOException ex) {
                throw new IllegalStateException("error reading osm file " + pathToMapTile, ex);
            } catch (SAXException ex) {
                throw new IllegalStateException("error parsing osm file " + pathToMapTile, ex);
            }
        }
    };

    public void setApiBaseUrl(String apiBaseUrl) {
        this._apiBaseUrl = apiBaseUrl;
    }

    public String getApiBaseUrl() {
        return _apiBaseUrl;
    }

    @Override
    public void checkInputs() {
        if (!_cacheDirectory.canWrite()) {
            throw new RuntimeException("Can't write to OSM cache: " + _cacheDirectory);
        }
    }
}
