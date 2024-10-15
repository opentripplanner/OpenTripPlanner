package org.opentripplanner.ext.vectortiles.layers;

import java.util.Set;
import org.opentripplanner.transit.model._data.TransitModelForTest;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TransitModel;

public class TestTransitService extends DefaultTransitService {

  public TestTransitService(TransitModel transitModel) {
    super(transitModel);
  }

  @Override
  public Set<Route> getRoutesForStop(StopLocation stop) {
    return Set.of(
      TransitModelForTest.route("1").withMode(TransitMode.RAIL).withGtfsType(100).build()
    );
  }
}
