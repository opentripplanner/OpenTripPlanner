package org.opentripplanner.openstreetmap;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.time.ZoneId;
import org.openstreetmap.osmosis.osmbinary.file.BlockInputStream;
import org.opentripplanner.datastore.api.DataSource;
import org.opentripplanner.datastore.api.FileType;
import org.opentripplanner.datastore.file.FileDataSource;
import org.opentripplanner.graph_builder.ConfiguredDataSource;
import org.opentripplanner.graph_builder.module.osm.OSMDatabase;
import org.opentripplanner.graph_builder.module.osm.WayPropertySet;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmDefaultsConfig;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractConfig;
import org.opentripplanner.graph_builder.module.osm.parameters.OsmExtractConfigBuilder;
import org.opentripplanner.openstreetmap.model.OSMProvider;
import org.opentripplanner.util.lang.ToStringBuilder;
import org.opentripplanner.util.logging.ProgressTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Parser for the OpenStreetMap PBF format. Parses files in three passes: First the relations, then
 * the ways, then the nodes are also loaded.
 */
public class OpenStreetMapProvider implements OSMProvider {

  private static final Logger LOG = LoggerFactory.getLogger(OpenStreetMapProvider.class);

  private final DataSource source;
  private final boolean cacheDataInMem;

  private final ZoneId zoneId;

  private boolean hasWarnedAboutMissingTimeZone = false;

  private final WayPropertySetSource wayPropertySetSource;

  private final WayPropertySet wayPropertySet;
  private byte[] cachedBytes = null;

  /** For tests */
  public OpenStreetMapProvider(File file, boolean cacheDataInMem) {
    this(new FileDataSource(file, FileType.OSM), cacheDataInMem);
  }

  public OpenStreetMapProvider(FileDataSource fileDataSource, boolean cacheDataInMem) {
    this(
      new ConfiguredDataSource<>(
        fileDataSource,
        new OsmExtractConfigBuilder().withSource(fileDataSource.uri()).build()
      ),
      new OsmDefaultsConfig(),
      cacheDataInMem
    );
  }

  public OpenStreetMapProvider(
    ConfiguredDataSource<OsmExtractConfig> osmExtractConfigConfiguredDataSource,
    OsmDefaultsConfig osmDefaultsConfig,
    boolean cacheDataInMem
  ) {
    this.source = osmExtractConfigConfiguredDataSource.dataSource();
    this.zoneId =
      osmExtractConfigConfiguredDataSource.config().timeZone().orElse(osmDefaultsConfig.timeZone);
    this.wayPropertySetSource =
      osmExtractConfigConfiguredDataSource
        .config()
        .osmWayPropertySet()
        .orElse(osmDefaultsConfig.osmWayPropertySetSource);
    this.wayPropertySet = new WayPropertySet();
    wayPropertySetSource.populateProperties(wayPropertySet);
    this.cacheDataInMem = cacheDataInMem;
  }

  public void readOSM(OSMDatabase osmdb) {
    try {
      OpenStreetMapParser parser = new OpenStreetMapParser(osmdb, this);

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
      .of(OpenStreetMapProvider.class)
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

  private void parsePhase(OpenStreetMapParser parser, OsmParserPhase phase) throws IOException {
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

  @Override
  public ZoneId getZoneId() {
    if (zoneId == null) {
      if (!hasWarnedAboutMissingTimeZone) {
        hasWarnedAboutMissingTimeZone = true;
        LOG.warn(
          "Missing time zone for OSM source {} - time-restricted entities will not be created, please configure it in the build-config.json",
          source.uri()
        );
      }
    }
    return zoneId;
  }

  @Override
  public WayPropertySetSource getWayPropertySetSource() {
    return wayPropertySetSource;
  }

  @Override
  public WayPropertySet getWayPropertySet() {
    return wayPropertySet;
  }
}
