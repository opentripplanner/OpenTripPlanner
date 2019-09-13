package org.opentripplanner.openstreetmap.impl;

import org.opentripplanner.openstreetmap.services.OpenStreetMapContentHandler;
import org.opentripplanner.openstreetmap.services.OpenStreetMapProvider;

import java.io.File;
import java.io.FileInputStream;

import crosby.binary.file.BlockInputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes:
 * First the relations, then the ways, then the nodes are also loaded.
 *
 * @see http://wiki.openstreetmap.org/wiki/PBF_Format
 * @see org.opentripplanner.openstreetmap.services.graph_builder.services.osm.OpenStreetMapContentHandler#biPhase
 * @since 0.4
 */
public class BinaryFileBasedOpenStreetMapProviderImpl implements OpenStreetMapProvider {

    private File path;

    public void readOSM(OpenStreetMapContentHandler handler) {
        try {
            BinaryOpenStreetMapParser parser = new BinaryOpenStreetMapParser(handler);

            FileInputStream input = new FileInputStream(path);
            parser.setParseNodes(false);
            parser.setParseWays(false);
            (new BlockInputStream(input, parser)).process();
            handler.doneFirstPhaseRelations();

            input = new FileInputStream(path);
            parser.setParseRelations(false);
            parser.setParseWays(true);
            (new BlockInputStream(input, parser)).process();
            handler.doneSecondPhaseWays();

            input = new FileInputStream(path);
            parser.setParseNodes(true);
            parser.setParseWays(false);
            (new BlockInputStream(input, parser)).process();
            handler.doneThirdPhaseNodes();
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + path, ex);        }
    }

    public void setPath(File path) {
        this.path = path;
    }

    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + path + ")";
    }

    @Override
    public void checkInputs() {
        if (!path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + path);
        }
    }
}
