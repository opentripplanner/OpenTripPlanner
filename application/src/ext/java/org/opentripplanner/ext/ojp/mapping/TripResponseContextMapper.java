package org.opentripplanner.ext.ojp.mapping;

import static org.opentripplanner.ext.ojp.mapping.TextMapper.internationalText;

import de.vdv.ojp20.PlaceStructure;
import de.vdv.ojp20.PlacesStructure;
import de.vdv.ojp20.ResponseContextStructure;
import de.vdv.ojp20.StopPlaceStructure;
import de.vdv.ojp20.StopPointStructure;
import java.util.Objects;
import java.util.stream.Stream;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.TripPlan;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.collection.StreamUtils;

class TripResponseContextMapper {

  private final StopRefMapper stopPointRefMapper;

  TripResponseContextMapper(StopRefMapper stopPointRefMapper) {
    this.stopPointRefMapper = stopPointRefMapper;
  }

  ResponseContextStructure map(TripPlan tripPlan) {
    var places = stopLocations(tripPlan).flatMap(this::place).toList();
    return new ResponseContextStructure().withPlaces(new PlacesStructure().withPlace(places));
  }

  private Stream<PlaceStructure> place(StopLocation stopLocation) {
    var stopPointStructure = new StopPointStructure()
      .withStopPointRef(stopPointRefMapper.stopPointRef(stopLocation))
      .withStopPointName(internationalText(stopLocation.getName()))
      .withPlannedQuay(internationalText(stopLocation.getPlatformCode()));
    if (stopLocation.isPartOfStation()) {
      stopPointStructure.withParentRef(
        stopPointRefMapper.stopPlaceRef(stopLocation.getParentStation())
      );
    }

    var stopPoint = new PlaceStructure()
      .withName(internationalText(stopLocation.getName()))
      .withStopPoint(stopPointStructure)
      .withGeoPosition(LocationMapper.map(stopLocation.getCoordinate()));

    if (stopLocation.isPartOfStation()) {
      var stopPlace = new PlaceStructure()
        .withName(internationalText(stopLocation.getParentStation().getName()))
        .withStopPlace(
          new StopPlaceStructure()
            .withStopPlaceRef(stopPointRefMapper.stopPlaceRef(stopLocation.getParentStation()))
            .withStopPlaceName(internationalText(stopLocation.getName()))
        )
        .withGeoPosition(LocationMapper.map(stopLocation.getCoordinate()));

      return Stream.of(stopPlace, stopPoint);
    } else {
      return Stream.of(stopPoint);
    }
  }

  private static Stream<StopLocation> stopLocations(TripPlan tripPlan) {
    return tripPlan.itineraries
      .stream()
      .flatMap(TripResponseContextMapper::stopLocations)
      .filter(Objects::nonNull)
      .distinct();
  }

  private static Stream<StopLocation> stopLocations(Itinerary itinerary) {
    return itinerary.legs().stream().flatMap(TripResponseContextMapper::stopLocations);
  }

  private static Stream<StopLocation> stopLocations(Leg leg) {
    var fromTo = Stream.of(leg.from().stop, leg.to().stop);
    var intermediate = StreamUtils.ofNullableCollection(leg.listIntermediateStops()).map(sa ->
      sa.place.stop
    );
    return Stream.concat(fromTo, intermediate);
  }
}
