package org.opentripplanner.model.projectinfo;

import org.junit.Test;
import org.opentripplanner.util.OtpAppException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.opentripplanner.model.projectinfo.GraphFileHeader.CHARSET;

public class GraphFileHeaderTest {
  private static final String HEADER = "OpenTripPlannerGraph;000000A;";
  private static final byte[] HEADER_BYTES = HEADER.getBytes(CHARSET);
  private static final GraphFileHeader SUBJECT = new GraphFileHeader("A");

  @Test
  public void headerLength() {
    assertEquals(29, GraphFileHeader.headerLength());
  }

  @Test
  public void parse() {
    assertEquals(SUBJECT, GraphFileHeader.parse(HEADER_BYTES));
  }

  @Test(expected = OtpAppException.class)
  public void parseToShort() {
    GraphFileHeader.parse(new byte[10]);
  }

  @Test(expected = OtpAppException.class)
  public void parseIllegalId() {
    String illegalVersionId = "€€€€€€";
    byte[] header = ("OpenTripPlannerGraph;"+ illegalVersionId +";").getBytes(CHARSET);
    GraphFileHeader.parse(header);
  }

  @Test
  public void header() {
    assertArrayEquals(HEADER_BYTES, SUBJECT.header());
  }

  @Test
  public void magicNumber() {
    assertEquals("OpenTripPlannerGraph", SUBJECT.magicNumber());
  }

  @Test
  public void otpSerializationVersionId() {
    assertEquals("A", SUBJECT.otpSerializationVersionId());
  }

  @Test
  public void otpSerializationVersionIdPadded() {
    assertEquals("000000A", SUBJECT.otpSerializationVersionIdPadded());
  }


  @Test
  public void asString() {
    assertEquals("OpenTripPlannerGraph;000000A;", SUBJECT.asString());
  }

  @Test
  public void testToString() {
    assertEquals(
        "OpenTripPlannerGraph;000000A;",
        SUBJECT.toString()
    );
  }

  @Test
  public void padId() {
    assertEquals("000000A", GraphFileHeader.padId("A"));
  }

  @Test
  public void stripId() {
    assertEquals("A", GraphFileHeader.stripId("000000A"));
  }

  @Test
  public void dump() {
    assertEquals("<empty>", GraphFileHeader.prettyBytesToString(null));
    assertEquals("<empty>", GraphFileHeader.prettyBytesToString(new byte[0]));
    assertEquals(
        "41 6C 66 61 2D 31  \"Alfa-1\"",
        GraphFileHeader.prettyBytesToString(new byte[]{'A', 'l', 'f', 'a', '-', '1'})
    );
  }
}