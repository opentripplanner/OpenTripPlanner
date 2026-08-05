package org.opentripplanner.ext.vectortiles.layers;

import java.util.Set;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitRepository;

public class TestTransitService extends DefaultTransitService {

  public TestTransitService(TransitRepository transitRepository) {
    super(transitRepository);
  }

  @Override
  public Set<Route> findRoutes(StopLocation stop) {
    return Set.of(
      TransitRepositoryForTest.route("1").withMode(TransitMode.RAIL).withGtfsType(100).build()
    );
  }
}
