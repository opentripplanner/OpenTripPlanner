package org.opentripplanner.ext.ojp.mapping;

import static org.opentripplanner.ext.ojp.mapping.TextMapper.internationalText;

import de.vdv.ojp20.DatedJourneyStructure;
import de.vdv.ojp20.JourneyRefStructure;
import de.vdv.ojp20.ModeStructure;
import de.vdv.ojp20.OperatingDayRefStructure;
import de.vdv.ojp20.siri.DirectionRefStructure;
import de.vdv.ojp20.siri.LineRefStructure;
import de.vdv.ojp20.siri.OperatorRefStructure;
import java.time.LocalDate;
import java.util.Objects;
import javax.annotation.Nullable;
import org.opentripplanner.api.model.transit.FeedScopedIdMapper;
import org.opentripplanner.model.TripTimeOnDate;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Trip;

class DatedJourneyMapper {

  private final FeedScopedIdMapper idMapper;
  private final StopRefMapper stopPointRefMapper;

  DatedJourneyMapper(FeedScopedIdMapper idMapper) {
    this.idMapper = idMapper;
    this.stopPointRefMapper = new StopRefMapper(idMapper);
  }

  DatedJourneyStructure datedJourney(TripTimeOnDate tripTimeOnDate, String lang) {
    var route = tripTimeOnDate.getTrip().getRoute();
    var firstStop = tripTimeOnDate.pattern().getStops().getFirst();
    var lastStop = tripTimeOnDate.pattern().getStops().getLast();
    return ofRoute(route, lang)
      .withJourneyRef(
        new JourneyRefStructure().withValue(idMapper.mapToApi(tripTimeOnDate.getTrip().getId()))
      )
      .withOperatingDayRef(
        new OperatingDayRefStructure().withValue(tripTimeOnDate.getServiceDay().toString())
      )
      .withOriginStopPointRef(stopPointRefMapper.stopPointRef(firstStop))
      .withOriginText(internationalText(firstStop.getName(), lang))
      .withDestinationStopPointRef(stopPointRefMapper.stopPointRef(lastStop))
      .withDestinationText(internationalText(tripTimeOnDate.getHeadsign(), lang))
      .withCancelled(tripTimeOnDate.getTripTimes().isCanceled())
      .withDirectionRef(
        new DirectionRefStructure().withValue(tripTimeOnDate.pattern().getDirection().toString())
      );
  }

  DatedJourneyStructure datedJourney(Trip trip, TripPattern pattern, LocalDate serviceDate) {
    var route = pattern.getRoute();
    var stops = pattern.getStops();
    var firstStop = stops.getFirst();
    var lastStop = stops.getLast();
    return ofRoute(route, null)
      .withJourneyRef(new JourneyRefStructure().withValue(idMapper.mapToApi(trip.getId())))
      .withOperatingDayRef(new OperatingDayRefStructure().withValue(serviceDate.toString()))
      .withOriginStopPointRef(stopPointRefMapper.stopPointRef(firstStop))
      .withOriginText(internationalText(firstStop.getName()))
      .withDestinationStopPointRef(stopPointRefMapper.stopPointRef(lastStop))
      .withDestinationText(
        internationalText(Objects.requireNonNullElse(trip.getHeadsign(), pattern.getTripHeadsign()))
      )
      .withDirectionRef(new DirectionRefStructure().withValue(pattern.getDirection().toString()));
  }

  private DatedJourneyStructure ofRoute(Route route, @Nullable String lang) {
    return new DatedJourneyStructure()
      .withLineRef(new LineRefStructure().withValue(idMapper.mapToApi(route.getId())))
      .withPublicCode(route.getName())
      .withMode(new ModeStructure().withPtMode(PtModeMapper.map(route.getMode())))
      .withPublishedServiceName(internationalText(route.getName(), lang))
      .withOperatorRef(
        new OperatorRefStructure().withValue(idMapper.mapToApi(route.getAgency().getId()))
      )
      .withRouteDescription(internationalText(route.getDescription(), lang));
  }
}
