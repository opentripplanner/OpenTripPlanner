package org.opentripplanner.model.projectinfo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.opentripplanner.model.projectinfo.GraphFileHeader.CHARSET;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.application.OtpAppException;

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

  @Test
  public void parseToShort() {
    assertThrows(OtpAppException.class, () -> GraphFileHeader.parse(new byte[10]));
  }

  @Test
  public void parseIllegalId() {
    String illegalVersionId = "€€€€€€";
    byte[] header = ("OpenTripPlannerGraph;" + illegalVersionId + ";").getBytes(CHARSET);
    assertThrows(OtpAppException.class, () -> GraphFileHeader.parse(header));
  }

  @Test
  public void header() {
    Assertions.assertArrayEquals(HEADER_BYTES, SUBJECT.header());
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
    assertEquals("OpenTripPlannerGraph;000000A;", SUBJECT.toString());
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
    assertEquals("[empty]", GraphFileHeader.prettyBytesToString(null));
    assertEquals("[empty]", GraphFileHeader.prettyBytesToString(new byte[0]));
    assertEquals(
      "41 6C 66 61 2D 31  \"Alfa-1\"",
      GraphFileHeader.prettyBytesToString(new byte[] { 'A', 'l', 'f', 'a', '-', '1' })
    );
  }
}
