package org.opentripplanner.ext.ojp.mapping;

import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.ojp.schema.ojp.StopPlaceRefStructure;
import org.opentripplanner.ojp.schema.siri.StopPointRefStructure;
import org.opentripplanner.transit.model.site.Station;
import org.opentripplanner.transit.model.site.StopLocation;

class StopRefMapper {

  private final FeedScopedIdMapper idMapper;

  StopRefMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
  }

  StopPointRefStructure stopPointRef(StopLocation stop) {
    return new StopPointRefStructure().withValue(idMapper.mapToApi(stop.getId()));
  }

  StopPlaceRefStructure stopPlaceRef(Station stop) {
    return new StopPlaceRefStructure().withValue(idMapper.mapToApi(stop.getId()));
  }
}
