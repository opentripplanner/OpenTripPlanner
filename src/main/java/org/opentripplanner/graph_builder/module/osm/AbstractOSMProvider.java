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
import org.opentripplanner.graph_builder.module.osm.contract.OSMParser;
import org.opentripplanner.graph_builder.module.osm.contract.OSMProvider;
import org.opentripplanner.graph_builder.module.osm.contract.PhaseAwareOSMEntityStore;
import org.opentripplanner.graph_builder.module.osm.exception.OSMProcessingException;
import org.opentripplanner.util.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractOSMProvider implements OSMProvider {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractOSMProvider.class);

  private final DataSource source;
  private final boolean cacheDataInMem;
  private byte[] cachedBytes = null;

  /** For tests */
  protected AbstractOSMProvider(File file, boolean cacheDataInMem) {
    this(new FileDataSource(file, FileType.OSM), cacheDataInMem);
  }

  protected AbstractOSMProvider(DataSource source, boolean cacheDataInMem) {
    this.source = source;
    this.cacheDataInMem = cacheDataInMem;
  }

  public DataSource getSource() {
    return source;
  }

  public boolean shouldCacheDataInMem() {
    return cacheDataInMem;
  }

  protected abstract OSMParser createParser(PhaseAwareOSMEntityStore osmdb);

  public void readOSM(PhaseAwareOSMEntityStore osmdb) {
    try {
      OSMParser parser = createParser(osmdb);

      parsePhase(parser, OSMParserPhase.RELATIONS);
      osmdb.doneFirstPhaseRelations();

      parsePhase(parser, OSMParserPhase.WAYS);
      osmdb.doneSecondPhaseWays();

      parsePhase(parser, OSMParserPhase.NODES);
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
      throw new OSMProcessingException("Can't read OSM path: " + source.path());
    }
  }

  @SuppressWarnings("Convert2MethodRef")
  private static InputStream track(OSMParserPhase phase, long size, InputStream inputStream) {
    // Keep logging lambda, replacing it with a method-ref will cause the
    // logging to report incorrect class and line number
    return ProgressTracker.track("Parse OSM " + phase, 1000, size, inputStream, LOG::info);
  }

  private void parsePhase(OSMParser parser, OSMParserPhase phase) throws IOException {
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

  private InputStream createInputStream(OSMParserPhase phase) {
    if (cacheDataInMem) {
      if (cachedBytes == null) {
        cachedBytes = source.asBytes();
      }
      return track(phase, cachedBytes.length, new ByteArrayInputStream(cachedBytes));
    }
    return track(phase, source.size(), source.asInputStream());
  }
}
