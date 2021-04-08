package org.opentripplanner.netex.mapping;

import org.opentripplanner.model.FlexStopLocation;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.FlexibleArea;
import org.rutebanken.netex.model.FlexibleStopPlace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FlexStopLocationMapper {

  private final FeedScopedIdFactory idFactory;

  private static final Logger LOG = LoggerFactory.getLogger(FlexStopLocationMapper.class);

  FlexStopLocationMapper(FeedScopedIdFactory idFactory) {
    this.idFactory = idFactory;
  }

  /**
   * Maps NeTEx FlexibleStopPlace to FlexStopLocation. This currently does not support
   * FlexLocationGroup, as an equivalent is not defined in the NeTEx Nordic profile.
   */
  FlexStopLocation map(FlexibleStopPlace flexibleStopPlace) {
    FlexStopLocation result = new FlexStopLocation(idFactory.createId(flexibleStopPlace.getId()));
    result.setName(flexibleStopPlace.getName().getValue());

    // Only one area allowed in NeTEx Nordic profile, get the first one
    Object area = flexibleStopPlace
        .getAreas()
        .getFlexibleAreaOrFlexibleAreaRefOrHailAndRideArea()
        .get(0);

    if (area instanceof FlexibleArea) {
      result.setGeometry(OpenGisMapper.mapGeometry(((FlexibleArea) area).getPolygon()));
    }
    else {
      LOG.warn(
          "FlexibleStopPlace {} not mapped. Hail and ride areas are not currently supported.",
          flexibleStopPlace.getId()
      );
      return null;
    }
    return result;
  }
}
