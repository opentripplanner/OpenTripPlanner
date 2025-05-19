package org.opentripplanner.netex.mapping;

import jakarta.xml.bind.JAXBElement;
import java.util.Map;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.netex.mapping.support.NetexMainAndSubMode;
import org.opentripplanner.netex.support.JourneyPatternHelper;
import org.opentripplanner.transit.model.basic.Accessibility;
import org.opentripplanner.transit.model.framework.DefaultEntityById;
import org.opentripplanner.transit.model.framework.EntityById;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.CarAccess;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.Trip;
import org.rutebanken.netex.model.DirectionTypeEnumeration;
import org.rutebanken.netex.model.JourneyPattern_VersionStructure;
import org.rutebanken.netex.model.LineRefStructure;
import org.rutebanken.netex.model.Route;
import org.rutebanken.netex.model.ServiceJourney;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This maps a NeTEx ServiceJourney to an OTP Trip. A ServiceJourney can be connected to a Line (OTP
 * Route) in two ways. Either directly from the ServiceJourney or through JourneyPattern → Route.
 * The former has precedent over the latter.
 */
class TripMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TripMapper.class);

  private final FeedScopedIdFactory idFactory;
  private final DataImportIssueStore issueStore;
  private final EntityById<org.opentripplanner.transit.model.network.Route> otpRouteById;
  private final ReadOnlyHierarchicalMap<String, Route> routeById;
  private final ReadOnlyHierarchicalMap<
    String,
    JourneyPattern_VersionStructure
  > journeyPatternsById;
  private final Map<String, FeedScopedId> serviceIds;
  private final EntityById<Operator> operatorsById;
  private final TransportModeMapper transportModeMapper = new TransportModeMapper();
  private final EntityById<Trip> mappedTrips = new DefaultEntityById<>();

  TripMapper(
    FeedScopedIdFactory idFactory,
    DataImportIssueStore issueStore,
    EntityById<Operator> operatorsById,
    EntityById<org.opentripplanner.transit.model.network.Route> otpRouteById,
    ReadOnlyHierarchicalMap<String, Route> routeById,
    ReadOnlyHierarchicalMap<String, JourneyPattern_VersionStructure> journeyPatternsById,
    Map<String, FeedScopedId> serviceIds
  ) {
    this.idFactory = idFactory;
    this.issueStore = issueStore;
    this.otpRouteById = otpRouteById;
    this.routeById = routeById;
    this.journeyPatternsById = journeyPatternsById;
    this.serviceIds = serviceIds;
    this.operatorsById = operatorsById;
  }

  /**
   * Map a service journey to a trip.
   * <p>
   *
   * @return valid trip or {@code null} if unable to map to a valid trip.
   */
  @Nullable
  Trip mapServiceJourney(ServiceJourney serviceJourney, Supplier<String> headsign) {
    FeedScopedId serviceId = serviceIds.get(serviceJourney.getId());

    if (serviceId == null) {
      LOG.warn("Unable to map ServiceJourney, missing Route. SJ id: {}", serviceJourney.getId());
      return null;
    }

    org.opentripplanner.transit.model.network.Route route = resolveRoute(serviceJourney);

    if (route == null) {
      LOG.warn(
        "Unable to map ServiceJourney, missing serviceId. SJ id: {}",
        serviceJourney.getId()
      );
      return null;
    }

    FeedScopedId id = idFactory.createId(serviceJourney.getId());

    if (mappedTrips.containsKey(id)) {
      return mappedTrips.get(id);
    }

    var wheelChairBoarding = WheelChairMapper.wheelchairAccessibility(
      serviceJourney.getAccessibilityAssessment(),
      Accessibility.NO_INFORMATION
    );

    var builder = Trip.of(id);
    builder.withRoute(route);
    builder.withServiceId(serviceId);
    builder.withWheelchairBoarding(wheelChairBoarding);

    if (serviceJourney.getPrivateCode() != null) {
      builder.withNetexInternalPlanningCode(serviceJourney.getPrivateCode().getValue());
    }

    builder.withShortName(serviceJourney.getPublicCode());
    builder.withOperator(findOperator(serviceJourney));

    if (serviceJourney.getTransportMode() != null) {
      NetexMainAndSubMode transitMode = null;
      try {
        transitMode = transportModeMapper.map(
          serviceJourney.getTransportMode(),
          serviceJourney.getTransportSubmode()
        );
      } catch (TransportModeMapper.UnsupportedModeException e) {
        issueStore.add(
          "UnsupportedModeInServiceJourney",
          "Unsupported mode in ServiceJourney. Mode: %s, sj: %s",
          e.mode,
          serviceJourney.getId()
        );
        return null;
      }
      builder.withMode(transitMode.mainMode());
      builder.withNetexSubmode(transitMode.subMode());
      builder.withCarsAllowed(
        transportModeMapper.mapCarsAllowed(serviceJourney.getTransportSubmode())
      );
    } else {
      builder.withCarsAllowed(transportModeMapper.mapCarsAllowed(route.getNetexSubmode()));
    }

    builder.withDirection(DirectionMapper.map(resolveDirectionType(serviceJourney)));

    builder.withNetexAlteration(
      TripServiceAlterationMapper.mapAlteration(serviceJourney.getServiceAlteration())
    );

    // TODO RTM - Instead of getting the first headsign from the StopTime this could be the
    //          - default behaviour of the TimetableRepository - So, in the NeTEx mapper we would just
    //          - ignore setting the headsign on the Trip.
    builder.withHeadsign(new NonLocalizedString(headsign.get()));

    return builder.build();
  }

  private DirectionTypeEnumeration resolveDirectionType(ServiceJourney serviceJourney) {
    Route netexRoute = lookUpNetexRoute(serviceJourney);
    if (netexRoute != null && netexRoute.getDirectionType() != null) {
      return netexRoute.getDirectionType();
    } else {
      return null;
    }
  }

  private Route lookUpNetexRoute(ServiceJourney serviceJourney) {
    if (serviceJourney.getJourneyPatternRef() != null) {
      JourneyPattern_VersionStructure journeyPattern = journeyPatternsById.lookup(
        serviceJourney.getJourneyPatternRef().getValue().getRef()
      );
      if (journeyPattern != null && journeyPattern.getRouteRef() != null) {
        String routeRef = journeyPattern.getRouteRef().getRef();
        return routeById.lookup(routeRef);
      }
    }
    return null;
  }

  private org.opentripplanner.transit.model.network.Route resolveRoute(
    ServiceJourney serviceJourney
  ) {
    String lineRef = null;
    // Check for direct connection to Line
    JAXBElement<? extends LineRefStructure> lineRefStruct = serviceJourney.getLineRef();

    if (lineRefStruct != null) {
      // Connect to Line referenced directly from ServiceJourney
      lineRef = lineRefStruct.getValue().getRef();
    } else if (serviceJourney.getJourneyPatternRef() != null) {
      // Connect to Line referenced through JourneyPattern->Route
      JourneyPattern_VersionStructure journeyPattern = journeyPatternsById.lookup(
        serviceJourney.getJourneyPatternRef().getValue().getRef()
      );
      lineRef = JourneyPatternHelper.getLineFromRoute(routeById, journeyPattern);
    }
    org.opentripplanner.transit.model.network.Route route = otpRouteById.get(
      idFactory.createId(lineRef)
    );

    if (route == null) {
      LOG.warn(
        "Unable to link ServiceJourney to Route. ServiceJourney id: {}, Line ref: {}",
        serviceJourney.getId(),
        lineRef
      );
    }
    return route;
  }

  @Nullable
  private Operator findOperator(ServiceJourney serviceJourney) {
    var opeRef = serviceJourney.getOperatorRef();
    if (opeRef == null) {
      return null;
    }
    return operatorsById.get(idFactory.createId(opeRef.getRef()));
  }
}
