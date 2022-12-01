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
  private OsmTagMapper.Source osmTagMapper;

  /**
   * The timezone to use to resolve opening hours in this extract.
   */
  private ZoneId timeZone;

  public OsmExtractParametersBuilder() {
    this.osmTagMapper = OsmExtractParameters.DEFAULT_OSM_TAG_MAPPER;
  }

  public OsmExtractParametersBuilder(OsmExtractParameters original) {
    this.osmTagMapper = original.osmTagMapper();
    this.timeZone = original.timeZone().orElse(null);
  }

  public OsmExtractParametersBuilder withSource(URI source) {
    this.source = source;
    return this;
  }

  public OsmExtractParametersBuilder withOsmTagMapper(OsmTagMapper.Source mapper) {
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

  public OsmTagMapper.Source getOsmTagMapper() {
    return osmTagMapper;
  }

  public ZoneId getTimeZone() {
    return timeZone;
  }

  public OsmExtractParameters build() {
    return new OsmExtractParameters(this);
  }
}
