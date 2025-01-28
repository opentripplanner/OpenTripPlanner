package org.opentripplanner.osm;

import crosby.binary.file.BlockInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.framework.application.OtpFileNames;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.graph_builder.module.osm.OsmDatabase;
import org.opentripplanner.osm.tagmapping.OsmTagMapper;
import org.opentripplanner.osm.tagmapping.OsmTagMapperSource;
import org.opentripplanner.osm.wayproperty.WayPropertySet;
import org.opentripplanner.utils.logging.ProgressTracker;
import org.opentripplanner.utils.tostring.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of the provider interface that reads OSM binary files from disk.
 */
public class DefaultOsmProvider implements OsmProvider {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultOsmProvider.class);

  private final DataSource source;
  private final boolean cacheDataInMem;

  private final ZoneId zoneId;

  private boolean hasWarnedAboutMissingTimeZone = false;

  private final OsmTagMapper osmTagMapper;

  private final boolean includeSubwayEntrances;
  private final WayPropertySet wayPropertySet;
  private byte[] cachedBytes = null;

  /** For tests */
  public DefaultOsmProvider(File file, boolean cacheDataInMem) {
    this(
      new FileDataSource(file, FileType.OSM),
      OsmTagMapperSource.DEFAULT,
      null,
      false,
      cacheDataInMem,
      DataImportIssueStore.NOOP
    );
  }

  public DefaultOsmProvider(
    DataSource dataSource,
    OsmTagMapperSource tagMapperSource,
    ZoneId zoneId,
    boolean includeSubwayEntrances,
    boolean cacheDataInMem,
    DataImportIssueStore issueStore
  ) {
    this.source = dataSource;
    this.zoneId = zoneId;
    this.osmTagMapper = tagMapperSource.getInstance();
    this.includeSubwayEntrances = includeSubwayEntrances;
    this.wayPropertySet = new WayPropertySet(issueStore);
    osmTagMapper.populateProperties(wayPropertySet);
    this.cacheDataInMem = cacheDataInMem;
  }

  public void readOsm(OsmDatabase osmdb) {
    try {
      OsmParser parser = new OsmParser(osmdb, this);

      parsePhase(parser, OsmParserPhase.Relations);
      osmdb.doneFirstPhaseRelations();

      parsePhase(parser, OsmParserPhase.Ways);
      osmdb.doneSecondPhaseWays();

      parsePhase(parser, OsmParserPhase.Nodes);
      osmdb.doneThirdPhaseNodes();
    } catch (Exception ex) {
      throw new IllegalStateException("error loading OSM from path " + source.path(), ex);
    }
  }

  @Override
  public String toString() {
    return ToStringBuilder
      .of(DefaultOsmProvider.class)
      .addObj("source", source)
      .addBool("cacheDataInMem", cacheDataInMem)
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
    return ProgressTracker.track("Parse OSM " + phase, 1000, size, inputStream, m -> LOG.info(m));
  }

  private void parsePhase(OsmParser parser, OsmParserPhase phase) throws IOException {
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

  public ZoneId getZoneId() {
    if (zoneId == null) {
      if (!hasWarnedAboutMissingTimeZone) {
        hasWarnedAboutMissingTimeZone = true;
        LOG.warn(
          "Missing time zone for OSM source {} - time-restricted entities will " +
          "not be created, please configure it in the {}",
          source.uri(),
          OtpFileNames.BUILD_CONFIG_FILENAME
        );
      }
    }
    return zoneId;
  }

  public OsmTagMapper getOsmTagMapper() {
    return osmTagMapper;
  }

  public WayPropertySet getWayPropertySet() {
    return wayPropertySet;
  }
}
