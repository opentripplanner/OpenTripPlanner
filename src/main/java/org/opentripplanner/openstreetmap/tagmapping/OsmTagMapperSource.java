package org.opentripplanner.openstreetmap.tagmapping;

/**
 * This is the list of {@link OsmTagMapper} sources. The enum provide a mapping between the enum
 * name and the actual implementation.
 */
public enum OsmTagMapperSource {
  DEFAULT,
  NORWAY,
  UK,
  FINLAND,
  GERMANY,
  ATLANTA,
  HOUSTON,
  PORTLAND;

  public OsmTagMapper getInstance() {
    return switch (this) {
      case DEFAULT -> new DefaultMapper();
      case NORWAY -> new NorwayMapper();
      case UK -> new UKMapper();
      case FINLAND -> new FinlandMapper();
      case GERMANY -> new GermanyMapper();
      case ATLANTA -> new AtlantaMapper();
      case HOUSTON -> new HoustonMapper();
      case PORTLAND -> new PortlandMapper();
    };
  }
}
