package org.opentripplanner.ext.carpooling.service;

import org.opentripplanner.TestOtpModel;
import org.opentripplanner.ext.carpooling.CarpoolingRepository;
import org.opentripplanner.ext.carpooling.internal.DefaultCarpoolingRepository;
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
    TransitService transitService = new DefaultTransitService(model.timetableRepository());
    var repository = new DefaultCarpoolingRepository();
    var service = new DefaultCarpoolingService(
      repository,
      STREET_LIMITATION_PARAMETERS,
      transitService,
      vertexCreationService
    );
    return new CarpoolingServiceTestContext(
      service,
      repository,
      new TransitServiceResolver(transitService)
    );
  }
}
