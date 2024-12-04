package org.opentripplanner.ext.vectortiles.layers;

import java.util.Set;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;

public class TestTransitService extends DefaultTransitService {

  public TestTransitService(TimetableRepository timetableRepository) {
    super(timetableRepository);
  }

  @Override
  public Set<Route> findRoutes(StopLocation stop) {
    return Set.of(
      TimetableRepositoryForTest.route("1").withMode(TransitMode.RAIL).withGtfsType(100).build()
    );
  }
}
