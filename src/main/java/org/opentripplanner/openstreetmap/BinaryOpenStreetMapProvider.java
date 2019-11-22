package org.opentripplanner.openstreetmap;

import org.opentripplanner.graph_builder.module.osm.OSMDatabase;

import java.io.File;
import java.io.FileInputStream;

import crosby.binary.file.BlockInputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes:
 * First the relations, then the ways, then the nodes are also loaded.
 */
public class BinaryOpenStreetMapProvider {

    private File path;

    public void readOSM(OSMDatabase osmdb) {
        try {
            BinaryOpenStreetMapParser parser = new BinaryOpenStreetMapParser(osmdb);

            FileInputStream input = new FileInputStream(path);
            parser.setParseNodes(false);
            parser.setParseWays(false);
            (new BlockInputStream(input, parser)).process();
            osmdb.doneFirstPhaseRelations();

            input = new FileInputStream(path);
            parser.setParseRelations(false);
            parser.setParseWays(true);
            (new BlockInputStream(input, parser)).process();
            osmdb.doneSecondPhaseWays();

            input = new FileInputStream(path);
            parser.setParseNodes(true);
            parser.setParseWays(false);
            (new BlockInputStream(input, parser)).process();
            osmdb.doneThirdPhaseNodes();
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + path, ex);
        }
    }

    public void setPath(File path) {
        this.path = path;
    }

    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + path + ")";
    }

    public void checkInputs() {
        if (!path.canRead()) {
            throw new RuntimeException("Can't read OSM path: " + path);
        }
    }
}
