package org.opentripplanner.openstreetmap;

import crosby.binary.file.BlockInputStream;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.graph_builder.module.osm.OSMDatabase;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes:
 * First the relations, then the ways, then the nodes are also loaded.
 */
public class BinaryOpenStreetMapProvider {
    private static final int PHASE_RELATIONS = 1;
    private static final int PHASE_WAYS = 2;
    private static final int PHASE_NODES = 3;

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

            parsePhase(parser, PHASE_RELATIONS);
            osmdb.doneFirstPhaseRelations();

            parsePhase(parser, PHASE_WAYS);
            osmdb.doneSecondPhaseWays();

            parsePhase(parser, PHASE_NODES);
            osmdb.doneThirdPhaseNodes();
        } catch (Exception ex) {
            throw new IllegalStateException("error loading OSM from path " + source.path(), ex);
        }
    }

    private void parsePhase(BinaryOpenStreetMapParser parser, int phase) throws IOException {
        parser.setParseRelations(phase == PHASE_RELATIONS);
        parser.setParseWays(phase == PHASE_WAYS);
        parser.setParseNodes(phase == PHASE_NODES);
        new BlockInputStream(createInputStream(), parser).process();
    }

    private InputStream createInputStream() {
        if(cacheDataImMem) {
            if(cachedBytes == null) {
                cachedBytes = source.asBytes();
            }
            return new ByteArrayInputStream(cachedBytes);
        }

        return source.asInputStream();
    }

    public String toString() {
        return "BinaryFileBasedOpenStreetMapProviderImpl(" + source.path() + ")";
    }

    public void checkInputs() {
        if (!source.exists()) {
            throw new RuntimeException("Can't read OSM path: " + source.path());
        }
    }
}
