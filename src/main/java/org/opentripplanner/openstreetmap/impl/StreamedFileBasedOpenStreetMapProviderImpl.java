package org.opentripplanner.openstreetmap.impl;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * @author Vincent Privat
 * @since 1.0
 */
public class StreamedFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File _path;

    /* (non-Javadoc)
     * @see org.opentripplanner.graph_builder.services.osm.OpenStreetMapProvider#readOSM(org.opentripplanner.graph_builder.services.osm.OpenStreetMapContentHandler)
     */
    @Override
    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            if (_path.getName().endsWith(".gz")) {
                InputStream in = new GZIPInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 1);
                handler.doneFirstPhaseRelations();

                in = new GZIPInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 2);
                handler.doneSecondPhaseWays();

                in = new GZIPInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 3);
                handler.doneThirdPhaseNodes();

            } else if (_path.getName().endsWith(".bz2")) {
                InputStream in = new BZip2CompressorInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 1);
                handler.doneFirstPhaseRelations();

                in = new BZip2CompressorInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 2);
                handler.doneSecondPhaseWays();

                in = new BZip2CompressorInputStream(new FileInputStream(_path));
                StreamedOpenStreetMapParser.parseMap(in, handler, 3);
                handler.doneThirdPhaseNodes();

            } else {
                StreamedOpenStreetMapParser.parseMap(_path, handler);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + _path, ex);
        }
    }

    public void setPath(File path) {
        _path = path;
    }

    public String toString() {
        return "StreamedFileBasedOpenStreetMapProviderImpl(" + _path + ")";
    }

    @Override
    public void checkInputs() {
        if (!_path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + _path);
        }
    }
}
