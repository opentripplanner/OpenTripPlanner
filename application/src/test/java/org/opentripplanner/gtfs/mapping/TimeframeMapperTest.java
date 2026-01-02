package org.opentripplanner.gtfs.mapping;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.onebusaway.gtfs.model.AgencyAndIdFactory.obaId;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.onebusaway.gtfs.model.Timeframe;

class TimeframeMapperTest {

  private static final IdFactory ID_FACTORY = new IdFactory("A");
  public static final LocalTime START = LocalTime.NOON;
  public static final LocalTime END = START.plusHours(1);

  @Test
  void map() {
    var tf = new Timeframe();
    tf.setTimeframeGroupId(obaId("1"));
    tf.setId(obaId("1"));
    tf.setStartTime(START);
    tf.setEndTime(END);
    tf.setServiceId("s1");

    var mapper = new TimeframeMapper(ID_FACTORY);
    var mapped = mapper.map(tf);
    assertEquals(START, mapped.startTime());
    assertEquals(END, mapped.endTime());
  }

  @Test
  void noValues() {
    var tf = new Timeframe();
    tf.setTimeframeGroupId(obaId("1"));
    tf.setId(obaId("1"));
    tf.setServiceId("s1");

    var mapper = new TimeframeMapper(ID_FACTORY);
    var mapped = mapper.map(tf);
    assertEquals(LocalTime.MIN, mapped.startTime());
    assertEquals(LocalTime.MAX, mapped.endTime());
  }
}
