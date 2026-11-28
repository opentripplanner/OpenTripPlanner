package org.opentripplanner.gtfs.mapping;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Area;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopAreaElement;
import org.opentripplanner.core.model.id.FeedScopedId;

class FareAreaMapperTest {

  private static final String FEED_ID = "F";
  private static final Area AREA_1 = area("area-1", "Area One");
  private static final Area AREA_2 = area("area-2", "Area Two");
  private static final Stop STOP_1 = stop("stop-1");
  private static final Stop STOP_2 = stop("stop-2");

  private final FareAreaMapper subject = new FareAreaMapper(new IdFactory(FEED_ID));

  @Test
  void mapArea() {
    var result = subject.map(AREA_1);

    assertEquals(new FeedScopedId(FEED_ID, "area-1"), result.getId());
    assertEquals("Area One", result.getName());
  }

  @Test
  void mapAreaWithoutName() {
    var result = subject.map(area("area-3", null));

    assertNull(result.getName());
  }

  @Test
  void mapSameAreaTwiceReturnsSameInstance() {
    assertSame(subject.map(AREA_1), subject.map(AREA_1));
  }

  @Test
  void stopInSeveralAreas() {
    subject.map(List.of(element(STOP_1, AREA_1), element(STOP_1, AREA_2)));

    assertThat(subject.areasFor(STOP_1)).containsExactly(subject.map(AREA_1), subject.map(AREA_2));
  }

  @Test
  void areaSharedByStopsIsOneInstance() {
    subject.map(List.of(element(STOP_1, AREA_1), element(STOP_2, AREA_1)));

    assertSame(
      subject.areasFor(STOP_1).iterator().next(),
      subject.areasFor(STOP_2).iterator().next()
    );
  }

  @Test
  void stopWithoutAssignment() {
    subject.map(List.of(element(STOP_1, AREA_1)));

    assertThat(subject.areasFor(STOP_2)).isEmpty();
  }

  private static Area area(String id, String name) {
    var area = new Area();
    area.setId(new AgencyAndId(FEED_ID, id));
    area.setName(name);
    return area;
  }

  private static Stop stop(String id) {
    var stop = new Stop();
    stop.setId(new AgencyAndId(FEED_ID, id));
    return stop;
  }

  private static StopAreaElement element(Stop stop, Area area) {
    var element = new StopAreaElement();
    element.setStop(stop);
    element.setArea(area);
    return element;
  }
}
