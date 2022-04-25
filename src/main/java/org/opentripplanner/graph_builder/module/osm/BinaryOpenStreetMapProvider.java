package org.opentripplanner.graph_builder.module.osm;

import com.google.common.base.MoreObjects;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import org.openstreetmap.osmosis.osmbinary.file.BlockInputStream;
import org.opentripplanner.datastore.DataSource;
import org.opentripplanner.datastore.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.graph_builder.module.osm.contract.OpenStreetMapParser;
import org.opentripplanner.graph_builder.module.osm.contract.OpenStreetMapProvider;
import org.opentripplanner.graph_builder.module.osm.contract.PhaseAwareOSMEntityStore;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes: First the relations, then
 * the ways, then the nodes are also loaded.
 */
public class BinaryOpenStreetMapProvider implements OpenStreetMapProvider {

  private static final Logger LOG = LoggerFactory.getLogger(BinaryOpenStreetMapProvider.class);

  private final DataSource source;
  private final boolean cacheDataInMem;
  private byte[] cachedBytes = null;

  /** For tests */
  public BinaryOpenStreetMapProvider(File file, boolean cacheDataInMem) {
    this(new FileDataSource(file, FileType.OSM), cacheDataInMem);
  }

  public BinaryOpenStreetMapProvider(DataSource source, boolean cacheDataInMem) {
    this.source = source;
    this.cacheDataInMem = cacheDataInMem;
  }

  public void readOSM(PhaseAwareOSMEntityStore osmdb) {
    try {
      OpenStreetMapParser parser = new BinaryOpenStreetMapParser(osmdb);

      parsePhase(parser, OsmParserPhase.RELATIONS);
      osmdb.doneFirstPhaseRelations();

      parsePhase(parser, OsmParserPhase.WAYS);
      osmdb.doneSecondPhaseWays();

      parsePhase(parser, OsmParserPhase.NODES);
      osmdb.doneThirdPhaseNodes();
    } catch (Exception ex) {
      throw new IllegalStateException("error loading OSM from path " + source.path(), ex);
    }
  }

  @Override
  public String toString() {
    return MoreObjects
      .toStringHelper(this)
      .add("source", source)
      .add("cacheDataInMem", cacheDataInMem)
      .add("cachedBytes", cachedBytes)
      .toString();
  }

  public void checkInputs() {
    if (!source.exists()) {
      throw new RuntimeException("Can't read OSM path: " + source.path());
    }
  }

  @SuppressWarnings("Convert2MethodRef")
  private static InputStream track(OsmParserPhase phase, long size, InputStream inputStream) {
    // Keep logging lambda, replacing it with a method-ref will cause the
    // logging to report incorrect class and line number
    return ProgressTracker.track("Parse OSM " + phase, 1000, size, inputStream, LOG::info);
  }

  private void parsePhase(OpenStreetMapParser parser, OsmParserPhase phase)
    throws IOException {
    parser.setPhase(phase);
    BlockInputStream in = null;
    try {
      in = new BlockInputStream(createInputStream(phase), parser);
      in.process();
    } finally {
      // Close
      try {
        if (in != null) {
          in.close();
        }
      } catch (Exception e) {
        LOG.error(e.getMessage(), e);
      }
    }
  }

  private InputStream createInputStream(OsmParserPhase phase) {
    if (cacheDataInMem) {
      if (cachedBytes == null) {
        cachedBytes = source.asBytes();
      }
      return track(phase, cachedBytes.length, new ByteArrayInputStream(cachedBytes));
    }
    return track(phase, source.size(), source.asInputStream());
  }
}
