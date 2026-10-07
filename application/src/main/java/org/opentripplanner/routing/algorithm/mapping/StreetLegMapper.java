package org.opentripplanner.routing.algorithm.mapping;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.ext.flex.FlexibleTransitLeg;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.framework.time.ZoneIdFallback;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.model.plan.leg.StreetLeg;
import org.opentripplanner.model.plan.walkstep.WalkStep;
import org.opentripplanner.model.plan.walkstep.verticaltransportation.VerticalTransportationUseFactory;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.service.streetdetails.StreetDetailsService;
import org.opentripplanner.service.vehiclerental.street.VehicleRentalPlaceVertex;
import org.opentripplanner.street.geometry.WgsCoordinate;
import org.opentripplanner.street.model.path.ExternalEdgeLeg;
import org.opentripplanner.street.model.path.PathLeg;
import org.opentripplanner.street.model.path.StreetLegPlace;
import org.opentripplanner.street.model.path.StreetPath;
import org.opentripplanner.street.model.path.step.StepEntrance;
import org.opentripplanner.street.model.path.step.StreetStep;
import org.opentripplanner.street.model.vertex.TemporaryStreetLocation;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.street.model.vertex.VehicleParkingEntranceVertex;
import org.opentripplanner.transit.SiteResolver;
import org.opentripplanner.transit.model.site.Entrance;

/**
 * Maps the legs of a {@link StreetPath} to the {@link Leg}s returned by the OTP APIs. This only
 * produces itineraries for non-transit searches, as well as the non-transit parts of itineraries
 * containing transit, while the whole transit itinerary is produced by
 * {@link RaptorPathToItineraryMapper}.
 * <p>
 * Splitting the path into legs and generating the turn-by-turn directions is done by the street
 * model, see {@link StreetPath#legs(double)}. This class adds the information that is not part of
 * the street model, like stops, entrances and levels.
 */
public class StreetLegMapper {

  private final SiteResolver siteResolver;
  private final ZoneId timeZone;
  private final VerticalTransportationUseFactory verticalTransportationUseFactory;
  private final double ellipsoidToGeoidDifference;

  public StreetLegMapper(
    SiteResolver siteResolver,
    ZoneId timeZone,
    StreetDetailsService streetDetailsService,
    double ellipsoidToGeoidDifference
  ) {
    this.siteResolver = siteResolver;
    this.timeZone = ZoneIdFallback.zoneId(timeZone);
    this.verticalTransportationUseFactory = new VerticalTransportationUseFactory(
      streetDetailsService
    );
    this.ellipsoidToGeoidDifference = ellipsoidToGeoidDifference;
  }

  /**
   * Generates {@link Leg}s from a {@link StreetPath}. The legs are returned in the order they are
   * traversed in the path.
   */
  public List<Leg> map(StreetPath path, RouteRequest request) {
    return map(path, request, null);
  }

  /**
   * Generates {@link Leg}s from a {@link StreetPath}. The legs are returned in the order they are
   * traversed in the path.
   *
   * @param startTime The legs are time shifted to so that the first leg starts at this time and the
   *                  next legs have the same delay as the first leg. If this is null, no time
   *                  shifting happens.
   */
  public List<Leg> map(StreetPath path, RouteRequest request, @Nullable ZonedDateTime startTime) {
    return map(path, request.listViaLocations(), startTime);
  }

  public List<Leg> map(
    StreetPath path,
    List<ViaLocation> viaLocations,
    @Nullable ZonedDateTime startTime
  ) {
    var pathLegs = path.legs(ellipsoidToGeoidDifference);
    if (pathLegs.isEmpty()) {
      return List.of();
    }
    var delay =
      startTime != null
        ? Duration.between(pathLegs.getFirst().startTime(), startTime.toInstant())
        : null;

    List<Leg> legs = new ArrayList<>();
    for (var pathLeg : pathLegs) {
      legs.add(mapLeg(pathLeg, viaLocations, delay));
    }
    return legs;
  }

  private Leg mapLeg(PathLeg pathLeg, List<ViaLocation> viaLocations, @Nullable Duration delay) {
    return switch (pathLeg) {
      case org.opentripplanner.street.model.path.StreetLeg streetLeg -> mapStreetLeg(
        streetLeg,
        viaLocations,
        delay
      );
      case ExternalEdgeLeg externalEdgeLeg -> mapFlexLeg(externalEdgeLeg, delay);
    };
  }

  private StreetLeg mapStreetLeg(
    org.opentripplanner.street.model.path.StreetLeg leg,
    List<ViaLocation> viaLocations,
    @Nullable Duration delay
  ) {
    var builder = StreetLeg.of()
      .withMode(leg.mode())
      .withStartTime(timeWithDelay(leg.startTime(), delay))
      .withEndTime(timeWithDelay(leg.endTime(), delay))
      .withFrom(mapPlace(leg.from(), viaLocations))
      .withTo(mapPlace(leg.to(), viaLocations))
      .withDistanceMeters(leg.distanceMeters())
      .withGeneralizedCost(leg.generalizedCost())
      .withGeometry(leg.geometry())
      .withElevationProfile(leg.elevationProfile())
      .withWalkSteps(leg.steps().stream().map(this::mapStep).toList())
      .withRentedVehicle(leg.rentedVehicle())
      .withWalkingBike(false);

    if (leg.vehicleRentalNetwork() != null) {
      builder.withVehicleRentalNetwork(leg.vehicleRentalNetwork());
    }
    return builder.build();
  }

  private Leg mapFlexLeg(ExternalEdgeLeg leg, @Nullable Duration delay) {
    if (!(leg.edge() instanceof FlexTripEdge flexEdge)) {
      throw new IllegalArgumentException("Unknown external edge: " + leg.edge());
    }
    return FlexibleTransitLeg.of()
      .withFlexTripEdge(flexEdge)
      .withFromStop(siteResolver.getStopLocation(flexEdge.fromStopId()))
      .withToStop(siteResolver.getStopLocation(flexEdge.toStopId()))
      .withStartTime(timeWithDelay(leg.startTime(), delay))
      .withEndTime(timeWithDelay(leg.endTime(), delay))
      .withGeneralizedCost(leg.generalizedCost())
      .build();
  }

  private Place mapPlace(StreetLegPlace place, List<ViaLocation> viaLocations) {
    return switch (place.vertex()) {
      case TransitStopVertex tsv -> {
        var stop = Objects.requireNonNull(siteResolver.getStop(tsv.getId()));
        yield Place.forStop(stop, ViaLocationTypeMapper.map(viaLocations, stop));
      }
      case VehicleRentalPlaceVertex v -> Place.forVehicleRentalPlace(v);
      case VehicleParkingEntranceVertex v -> Place.forVehicleParkingEntrance(
        v,
        place.realTimeVehicleParking()
      );
      case TemporaryStreetLocation v -> Place.normal(
        v,
        place.name(),
        ViaLocationTypeMapper.map(viaLocations, v)
      );
      default -> Place.normal(place.vertex(), place.name());
    };
  }

  private WalkStep mapStep(StreetStep step) {
    var builder = WalkStep.builder()
      .withStartLocation(step.startLocation())
      .withRelativeDirection(step.relativeDirection())
      .withAbsoluteDirection(step.absoluteDirection())
      .withDirectionText(step.directionText())
      .withExit(step.highwayExit())
      .withEntrance(mapEntrance(step.entrance()))
      .withVerticalTransportationUse(
        step.verticalTransportation() == null
          ? null
          : verticalTransportationUseFactory.create(step.verticalTransportation())
      )
      .addElevation(step.elevationProfile())
      .withNameIsDerived(step.nameIsDerived())
      .withWalkingBike(step.walkingBike())
      .withArea(step.area())
      .withStayOn(step.stayOn())
      .withAngle(step.angle())
      .addDistance(step.distance());
    step.edges().forEach(builder::addEdge);
    return builder.build();
  }

  @Nullable
  private Entrance mapEntrance(@Nullable StepEntrance entrance) {
    return switch (entrance) {
      case null -> null;
      case StepEntrance.TransitEntrance e -> siteResolver.getEntrance(e.id());
      case StepEntrance.StationEntrance e -> Entrance.of(e.vertex().id())
        .withCode(e.vertex().code())
        .withCoordinate(new WgsCoordinate(e.vertex().getCoordinate()))
        .withWheelchairAccessibility(e.vertex().wheelchairAccessibility())
        .build();
    };
  }

  private ZonedDateTime timeWithDelay(Instant time, @Nullable Duration delay) {
    var zonedTime = time.atZone(timeZone);
    return delay == null ? zonedTime : zonedTime.plus(delay);
  }
}
