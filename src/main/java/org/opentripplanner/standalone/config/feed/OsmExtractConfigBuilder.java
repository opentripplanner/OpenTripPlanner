package org.opentripplanner.standalone.config.feed;

import java.net.URI;
import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.WayPropertySetSource;
import org.opentripplanner.standalone.config.NodeAdapter;

/**
 * Configure an OpenStreetMap extract.
 */
public class OsmExtractConfigBuilder {

  /**
   * URI to the source file.
   */
  private URI source;

  /**
   * Custom OSM way properties for this extract.
   */
  private WayPropertySetSource osmWayPropertySet;

  /**
   * The timezone to use to resolve opening hours in this extract.
   */
  private ZoneId timeZone;

  public static OsmExtractConfigBuilder of(NodeAdapter config) {
    OsmExtractConfigBuilder osmExtractConfigBuilder = new OsmExtractConfigBuilder();
    osmExtractConfigBuilder.source = config.asUri("source");
    String osmTagMapping = config.asText("osmTagMapping", null);
    if (osmTagMapping != null) {
      osmExtractConfigBuilder.osmWayPropertySet = WayPropertySetSource.fromConfig(osmTagMapping);
    }
    osmExtractConfigBuilder.timeZone = config.asZoneId("timeZone", null);
    return osmExtractConfigBuilder;
  }

  public OsmExtractConfigBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public OsmExtractConfigBuilder withOsmWayPropertySet(WayPropertySetSource osmWayPropertySet) {
    this.osmWayPropertySet = osmWayPropertySet;
    return this;
  }

  public OsmExtractConfigBuilder withTimeZone(ZoneId timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  public URI getSource() {
    return source;
  }

  public WayPropertySetSource getOsmWayPropertySet() {
    return osmWayPropertySet;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }

  public OsmExtractConfig build() {
    return new OsmExtractConfig(this);
  }
}
