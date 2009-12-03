/* This program is free software: you can redistribute it and/or
   modify it under the terms of the GNU Lesser General Public License
   as published by the Free Software Foundation, either version 3 of
   the License, or (at your option) any later version.
   
   This program is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
   
   You should have received a copy of the GNU General Public License
   along with this program.  If not, see <http://www.gnu.org/licenses/>. 
*/

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
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.graph_builder.services.RegionsSource;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Envelope;

public class RegionBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {
    
    private static Logger _log = LoggerFactory.getLogger(RegionBasedOpenStreetMapProviderImpl.class);

    private RegionsSource _regionsSource;

    private File _cacheDirectory;
    
    public void setRegionsSource(RegionsSource regionsSource) {
        _regionsSource = regionsSource;
    }
    
    public void setCacheDirectory(File cacheDirectory) {
        _cacheDirectory = cacheDirectory;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {

        OSMDownloader downloader = new OSMDownloader();
        
        if( _cacheDirectory != null)
            downloader.setCacheDirectory(_cacheDirectory);
        
        DownloadHandler downloadHandler = new DownloadHandler(handler);

        try {
            int regionIndex = 0;
            for (Envelope region : _regionsSource.getRegions()) {
                downloader.visitRegion(region, downloadHandler);
                _log.debug("regions=" + regionIndex);
                regionIndex++;
            }
        } catch (IOException ex) {
            throw new IllegalStateException("error downloading osm", ex);
        }
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
            } catch (Exception ex) {
                throw new IllegalStateException("error parsing osm file " + pathToMapTile, ex);
            }
        }
    };

}
