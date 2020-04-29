package org.opentripplanner.openstreetmap;

import com.esotericsoftware.kryo.io.Input;
import java.io.InputStream;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.graph_builder.module.osm.OSMDatabase;

import java.io.File;
import java.io.FileInputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes:
 * First the relations, then the ways, then the nodes are also loaded.
 */
public class BinaryOpenStreetMapProvider {

    private final DataSource source;
    private final boolean cacheDataImMem;
    private byte[] cachedBytes = null;


    /** For tests */
    public BinaryOpenStreetMapProvider(File file, boolean cacheDataImMem) {
        this(new FileDataSource(file, FileType.OSM), cacheDataImMem);
    }

    public BinaryOpenStreetMapProvider(DataSource source, boolean cacheDataImMem) {
        this.source = source;
        this.cacheDataImMem = cacheDataImMem;
    }

    public void readOSM(OSMDatabase osmdb) {
        try {
            BinaryOpenStreetMapParser parser = new BinaryOpenStreetMapParser(osmdb);

            InputStream input = source.asInputStream();
            parser.setParseNodes(false);
            parser.setParseWays(false);
            parser.process(input);
            osmdb.doneFirstPhaseRelations();

            input = source.asInputStream();
            parser.setParseRelations(false);
            parser.setParseWays(true);
            parser.process(input);
            osmdb.doneSecondPhaseWays();

            input = source.asInputStream();
            parser.setParseNodes(true);
            parser.setParseWays(false);
            parser.process(input);
            osmdb.doneThirdPhaseNodes();
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from source " + source, ex);
        }
    }

    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + source + ")";
    }

    public void checkInputs() {
        if (!source.exists()) {
            throw new RuntimeException("Can't read OSM source: " + source);
        }
    }
}
