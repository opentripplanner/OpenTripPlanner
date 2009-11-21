package org.opentripplanner.graph_builder.impl.osm;

import java.io.File;

import org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler;
import org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider;

public class FileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    public void setPath(File path) {
        _path = path;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            OpenStreetMapParser parser = new OpenStreetMapParser();
            parser.parseMap(_path, handler);
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }
}
