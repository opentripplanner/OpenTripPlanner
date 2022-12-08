package org.opentripplanner.graph_builder.module.osm.parameters;

import java.net.URI;
import java.time.ZoneId;
import org.opentripplanner.openstreetmap.tagmapping.OsmTagMapperSource;

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
  private OsmTagMapperSource osmTagMapper;

  /**
   * The timezone to use to resolve opening hours in this extract.
   */
  private ZoneId timeZone;

  public OsmExtractParametersBuilder() {
    this.osmTagMapper = OsmExtractParameters.DEFAULT_OSM_TAG_MAPPER;
    this.timeZone = OsmExtractParameters.DEFAULT_TIME_ZONE;
  }

  public OsmExtractParametersBuilder(OsmExtractParameters original) {
    this.osmTagMapper = original.osmTagMapper();
    this.timeZone = original.timeZone();
  }

  public OsmExtractParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public OsmExtractParametersBuilder withOsmTagMapper(OsmTagMapperSource mapper) {
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

  public OsmTagMapperSource getOsmTagMapper() {
    return osmTagMapper;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }

  public OsmExtractParameters build() {
    return new OsmExtractParameters(this);
  }
}
