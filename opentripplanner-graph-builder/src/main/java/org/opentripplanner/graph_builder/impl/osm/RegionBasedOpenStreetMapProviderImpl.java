package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.opentripplanner.graph_builder.services.RegionsSource;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;

import com.vividsolutions.jts.geom.Envelope;

public class RegionBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

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
            for (Envelope region : _regionsSource.getRegions())
                downloader.visitRegion(region, downloadHandler);
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
