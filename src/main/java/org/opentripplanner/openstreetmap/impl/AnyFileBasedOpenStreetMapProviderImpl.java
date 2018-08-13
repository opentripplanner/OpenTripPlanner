package org.opentripplanner.openstreetmap.impl;

import java.io.File;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

public class AnyFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    public void setPath(File path) {
        _path = path;
    }

    public AnyFileBasedOpenStreetMapProviderImpl (File file) {
        this.setPath(file);
    }
    
    public AnyFileBasedOpenStreetMapProviderImpl() { };

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            if (_path.getName().endsWith(".pbf")) {
                BinaryFileBasedOpenStreetMapProviderImpl p = new BinaryFileBasedOpenStreetMapProviderImpl();
                p.setPath(_path);
                p.readOSM(handler);
            } else {
                StreamedFileBasedOpenStreetMapProviderImpl p = new StreamedFileBasedOpenStreetMapProviderImpl();
                p.setPath(_path);
                p.readOSM(handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }

    public String toString() {
        return "AnyFileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + _path);
        }
    }
}
