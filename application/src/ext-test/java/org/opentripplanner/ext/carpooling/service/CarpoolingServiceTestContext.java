package org.opentripplanner.ext.carpooling.service;

import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
import org.opentripplanner.ext.carpooling.model.CarpoolTrip;
import org.opentripplanner.ext.carpooling.routing.CarpoolTripVertexResolver;
import org.opentripplanner.ext.carpooling.util.CarReachableVertexSnapper;
import org.opentripplanner.routing.linking.VertexLinkerTestFactory;
import org.opentripplanner.routing.linking.internal.VertexCreationService;
import org.opentripplanner.street.service.StreetLimitationParametersService;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.transit.service.TransitServiceResolver;

/**
 * Wires a {@link DefaultCarpoolingService} with an empty repository and in-memory dependencies on
 * top of a {@link org.opentripplanner.routing.algorithm.GraphRoutingTest} street graph model.
 */
record CarpoolingServiceTestContext(
  DefaultCarpoolingService service,
  CarpoolingRepository repository,
  CarpoolTripVertexResolver resolver,
  TransitServiceResolver transitServiceResolver
) {
  private static final StreetLimitationParametersService STREET_LIMITATION_PARAMETERS =
    new StreetLimitationParametersService() {
      @Override
      public float maxCarSpeed() {
        return 40.0f;
      }

      @Override
      public int maxAreaNodes() {
        return 500;
      }

      @Override
      public float getBestWalkSafety() {
        return 1;
      }

      @Override
      public float getBestBikeSafety() {
        return 1;
      }
    };

  static CarpoolingServiceTestContext of(TestOtpModel model) {
    var vertexCreationService = new VertexCreationService(
      VertexLinkerTestFactory.of(model.graph())
    );
    TransitService transitService = new DefaultTransitService(model.transitRepository());
    var repository = new DefaultCarpoolingRepository();
    var carReachableVertexSnapper = CarReachableVertexSnapper.createDefault();
    var resolver = new CarpoolTripVertexResolver(vertexCreationService, carReachableVertexSnapper);
    var service = new DefaultCarpoolingService(
      repository,
      STREET_LIMITATION_PARAMETERS,
      vertexCreationService,
      carReachableVertexSnapper
    );
    return new CarpoolingServiceTestContext(
      service,
      repository,
      resolver,
      new TransitServiceResolver(transitService)
    );
  }

  /**
   * Resolves the trip's route points against the test graph and stores the result; fails the test
   * if any point cannot be resolved.
   */
  void upsertTrip(CarpoolTrip trip) {
    var tripWithVertices = resolver.resolve(trip);
    if (tripWithVertices == null) {
      throw new IllegalStateException(
        "Trip %s has a route point that does not resolve to a car-reachable vertex on the test graph".formatted(
          trip.getId()
        )
      );
    }
    repository.upsertCarpoolTrip(tripWithVertices);
  }
}
