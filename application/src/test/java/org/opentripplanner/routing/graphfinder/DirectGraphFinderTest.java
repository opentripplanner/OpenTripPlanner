package org.opentripplanner.routing.graphfinder;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opentripplanner.TestOtpModel;
import org.opentripplanner.routing.algorithm.GraphRoutingTest;
import org.opentripplanner.street.model.vertex.TransitStopVertex;
import org.opentripplanner.transit.service.SiteRepository;

class DirectGraphFinderTest extends GraphRoutingTest {

  private SiteRepository siteRepository;

  private TransitStopVertex S1;
  private TransitStopVertex S2;
  private TransitStopVertex S3;

  @BeforeEach
  protected void setUp() throws Exception {
    TestOtpModel model = modelOf(
      new Builder() {
        @Override
        public void build() {
          S1 = stop("S1", 47.500, 19);
          S2 = stop("S2", 47.510, 19);
          S3 = stop("S3", 47.520, 19);
        }
      }
    );
    siteRepository = model.timetableRepository().getSiteRepository();
  }

  @Test
  void findClosestStops() {
    var ns1 = new NearbyStop(siteRepository.getRegularStop(S1.getId()), 0, null, null);
    var ns2 = new NearbyStop(siteRepository.getRegularStop(S2.getId()), 1112, null, null);

    var subject = new DirectGraphFinder(siteRepository::findRegularStops);
    var coordinate = new Coordinate(19.000, 47.500);
    assertEquals(List.of(ns1), subject.findClosestStops(coordinate, 100));

    assertEquals(List.of(ns1, ns2), subject.findClosestStops(coordinate, 2000));
  }
}
