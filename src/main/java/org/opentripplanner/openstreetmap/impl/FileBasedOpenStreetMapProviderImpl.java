package org.opentripplanner.openstreetmap.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

public class FileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File path;

    public void setPath(File path) {
        this.path = path;
    }

    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            OpenStreetMapParser parser = new OpenStreetMapParser();
            if (path.getName().endsWith(".gz")) {
                InputStream in = new GZIPInputStream(new FileInputStream(path));
                parser.parseMap(in, handler);
            } else if (path.getName().endsWith(".bz2")) {
                BZip2CompressorInputStream in = new BZip2CompressorInputStream(new FileInputStream(path));
                parser.parseMap(in, handler);
            } else {
                parser.parseMap(path, handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + path, ex);
        }
    }

    public String toString() {
        return "FileBasedOpenStreetMapProviderImpl(" + path + ")";
    }

    @Override
    public void checkInputs() {
        if (!path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + path);
        }
    }
}
