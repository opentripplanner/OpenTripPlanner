package org.opentripplanner.routing.refetch;

import static org.opentripplanner.street.model.RentalFormFactor.BICYCLE;
import static org.opentripplanner.street.model.RentalFormFactor.CAR;
import static org.opentripplanner.street.model.RentalFormFactor.SCOOTER;

import java.time.LocalDate;
import java.util.Map;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.core.model.i18n.NonLocalizedString;
import org.opentripplanner.core.model.id.FeedScopedId;
import org.opentripplanner.model.GenericLocation;
import org.opentripplanner.model.plan.legreference.ScheduledTransitLegReference;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.request.StreetRequest;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.linking.LinkingContextFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.routing.refetch.ItineraryRefetchServiceTest.GraphBuilder;
import org.opentripplanner.routing.refetch.ItineraryRefetchServiceTest.VertexRef;
import org.opentripplanner.service.vehiclerental.GeofencingZoneService;
import org.opentripplanner.service.vehiclerental.model.RentalVehicleType;
import org.opentripplanner.service.vehiclerental.model.TestFreeFloatingRentalVehicleBuilder;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalPlace;
import org.opentripplanner.service.vehiclerental.model.VehicleRentalStation;
import org.opentripplanner.street.graph.Graph;
import org.opentripplanner.street.linking.VertexLinker;
import org.opentripplanner.street.linking.VisibilityMode;
import org.opentripplanner.street.model.RentalFormFactor;
import org.opentripplanner.street.model.StreetMode;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.model.TransitTestEnvironment;
import org.opentripplanner.transit.model.TransitTestEnvironmentBuilder;
import org.opentripplanner.transit.model.TripInput;
import org.opentripplanner.transit.model.site.RegularStop;
import org.opentripplanner.transit.service.NoopSiteResolver;
import org.opentripplanner.transit.transfer.regular.FixedRegularTransfers;

/// A trip from stop A to stop D, with rental vehicles on the way to A and from D, for testing the
/// refetch of itineraries with a rental access and egress.
///
/// ```
/// Access:  ORIGIN -100m- R1 -3000m- R2 -100m- stop A
///                                   |
///                                 100m- R2_SPUR
/// Egress:  stop D -100m- R3 -3000m- R4 -100m- DESTINATION
/// ```
///
/// R1 to R4 each have a bicycle and a car rental station of {@link #NETWORK}, R1 and R3 a
/// free-floating scooter. R1 and R2_SPUR also have a car rental station of {@link #NETWORK_2},
/// which is a longer way to stop A. The 3200 m are a long walk, so the cheapest access and egress
/// use a rental vehicle.
public final class RentalRefetchTestModel {

  public static final LocalDate SERVICE_DATE = LocalDate.of(2020, 3, 3);
  public static final String NETWORK = "network";
  public static final String NETWORK_2 = "network 2";

  private static final TransitTestEnvironmentBuilder ENV_BUILDER = TransitTestEnvironment.of(
    SERVICE_DATE
  );
  public static final RegularStop STOP_A = ENV_BUILDER.stop("A");
  public static final RegularStop STOP_D = ENV_BUILDER.stop("D");
  public static final TransitTestEnvironment TRANSIT_ENV = ENV_BUILDER.addTrip(
    TripInput.of("trip1").addStop(STOP_A, "10:00").addStop(STOP_D, "12:00")
  ).build();

  private static final GraphBuilder G = GraphBuilder.of();
  private static final VertexRef ORIGIN_VERTEX = G.vertex();
  private static final VertexRef R1 = G.vertex();
  private static final VertexRef R2 = G.vertex();
  private static final VertexRef VA = G.linkStop(STOP_A);
  private static final VertexRef VD = G.linkStop(STOP_D);
  private static final VertexRef R3 = G.vertex();
  private static final VertexRef R4 = G.vertex();
  private static final VertexRef DESTINATION_VERTEX = G.vertex();
  private static final VertexRef R2_SPUR = G.vertex();

  static {
    ORIGIN_VERTEX.street(R1).meters(100);
    R1.street(R2).meters(3000);
    R2.street(VA).meters(100);
    VD.street(R3).meters(100);
    R3.street(R4).meters(3000);
    R4.street(DESTINATION_VERTEX).meters(100);
    R2.street(R2_SPUR).meters(100);

    Map.of(R1, "R1", R2, "R2", R3, "R3", R4, "R4").forEach((at, name) -> {
      G.rentalPlace(at, station(NETWORK, "bicycle " + name, at, BICYCLE, 2), BICYCLE);
      G.rentalPlace(at, station(NETWORK, "car " + name, at, CAR, 2), CAR);
    });
    G.rentalPlace(R1, scooter("scooter R1", R1), SCOOTER);
    G.rentalPlace(R3, scooter("scooter R3", R3), SCOOTER);
    G.rentalPlace(R1, station(NETWORK_2, "car 2 R1", R1, CAR, 2), CAR);
    G.rentalPlace(R2_SPUR, station(NETWORK_2, "car 2 R2 spur", R2_SPUR, CAR, 2), CAR);
  }

  public static final Graph GRAPH = G.build();
  public static final GenericLocation ORIGIN = GenericLocation.fromCoordinate(
    ORIGIN_VERTEX.coord()
  );
  public static final GenericLocation DESTINATION = GenericLocation.fromCoordinate(
    DESTINATION_VERTEX.coord()
  );

  private RentalRefetchTestModel() {}

  /// The ride on trip1 from stop A to stop D.
  public static ScheduledTransitLegReference legRef() {
    var tripData = TRANSIT_ENV.tripData("trip1");
    var stops = tripData.tripPattern().getStops();
    return new ScheduledTransitLegReference(
      tripData.trip().getId(),
      SERVICE_DATE,
      stops.indexOf(STOP_A),
      stops.indexOf(STOP_D),
      STOP_A.getId(),
      STOP_D.getId(),
      null
    );
  }

  /// A request from {@link #ORIGIN} to {@link #DESTINATION} with {@code mode} as access and
  /// egress.
  public static RouteRequest request(StreetMode mode) {
    return RouteRequest.of()
      .withFrom(ORIGIN)
      .withTo(DESTINATION)
      .withJourney(journey ->
        journey.withAccess(new StreetRequest(mode)).withEgress(new StreetRequest(mode))
      )
      .withPreferences(p -> p.withWalk(w -> w.withSpeed(2)))
      .buildRequest();
  }

  public static ItineraryRefetchService createRefetchService() {
    return createRefetchService(GRAPH, GeofencingZoneService.EMPTY);
  }

  public static ItineraryRefetchService createRefetchService(
    Graph graph,
    GeofencingZoneService geofencingZoneService
  ) {
    var vertexCreationService = new VertexCreationService(
      new VertexLinker(
        graph,
        GeofencingZoneService.EMPTY,
        VisibilityMode.TRAVERSE_AREA_EDGES,
        10,
        false
      )
    );
    var linkingContextFactory = new LinkingContextFactory(
      graph,
      vertexCreationService,
      new NoopSiteResolver()
    );
    return new ItineraryRefetchService(
      graph,
      TRANSIT_ENV.transitService(),
      new TransitAlertServiceImpl(),
      FixedRegularTransfers.of().snapshot(),
      null,
      linkingContextFactory,
      StreetLimitationParametersService.DEFAULT,
      geofencingZoneService
    );
  }

  /// A free-floating scooter of {@link #NETWORK} at {@code at}.
  public static VehicleRentalPlace scooter(String name, VertexRef at) {
    return TestFreeFloatingRentalVehicleBuilder.of()
      .withVehicleScooter()
      .withLatitude(at.coord().latitude())
      .withLongitude(at.coord().longitude())
      .build()
      .copyOf()
      .withId(new FeedScopedId(NETWORK, name))
      .withName(new NonLocalizedString(name))
      .build();
  }

  /// A station with room for two vehicles, where a rented vehicle may be kept at the destination.
  public static VehicleRentalStation station(
    String network,
    String name,
    VertexRef at,
    RentalFormFactor formFactor,
    int vehiclesAvailable
  ) {
    var vehicleType = RentalVehicleType.of()
      .withId(new FeedScopedId(network, formFactor.name()))
      .withName(I18NString.of(formFactor.name()))
      .withFormFactor(formFactor)
      .withPropulsionType(RentalVehicleType.PropulsionType.HUMAN)
      .build();
    return VehicleRentalStation.of()
      .withId(new FeedScopedId(network, name))
      .withName(new NonLocalizedString(name))
      .withLatitude(at.coord().latitude())
      .withLongitude(at.coord().longitude())
      .withVehiclesAvailable(vehiclesAvailable)
      .withSpacesAvailable(2)
      .withVehicleTypesAvailable(Map.of(vehicleType, vehiclesAvailable))
      .withVehicleSpacesAvailable(Map.of(vehicleType, 2))
      .withIsArrivingInRentalVehicleAtDestinationAllowed(true)
      .build();
  }
}
