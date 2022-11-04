package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import org.opentripplanner.graph_builder.module.osm.tagmapping.OsmTagMapper;

/**
 * Configure an OpenStreetMap extract.
 */
public class OsmExtractParametersBuilder {

  /**
   * URI to the source file.
   */
  private URI source;

  /**
   * Custom OSM way properties for this extract.
   */
  private OsmTagMapper osmTagMapper;

  /**
   * The timezone to use to resolve opening hours in this extract.
   */
  private ZoneId timeZone;

  public OsmExtractParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public OsmExtractParametersBuilder withOsmTagMapper(OsmTagMapper mapper) {
    this.osmTagMapper = mapper;
    return this;
  }

  public OsmExtractParametersBuilder withTimeZone(ZoneId timeZone) {
    this.timeZone = timeZone;
    return this;
  }

  public URI getSource() {
    return source;
  }

  public OsmTagMapper getOsmTagMapper() {
    return osmTagMapper;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }

  public OsmExtractParameters build() {
    return new OsmExtractParameters(this);
  }
}
