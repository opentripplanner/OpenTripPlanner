package org.opentripplanner.model.projectinfo;

import org.junit.Test;

import static org.junit.Assert.*;

public class GraphFileHeaderTest {
  private static final String HEADER = "OpenTripPlannerGraph-01;00000A;";
  private static final byte[] HEADER_BYTES = HEADER.getBytes(GraphFileHeader.CHARSET);
  private static final GraphFileHeader SUBJECT = new GraphFileHeader("A");

  @Test
  public void headerLength() {
    assertEquals(31, GraphFileHeader.headerLength());
  }

  @Test
  public void parse() {
    assertEquals(SUBJECT, GraphFileHeader.parse(HEADER_BYTES));
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseToShort() {
    GraphFileHeader.parse(new byte[10]);
  }

  @Test(expected = IllegalArgumentException.class)
  public void parseIllegalId() {
    GraphFileHeader.parse("OpenTripPlannerGraph-01;#$%&/(;".getBytes(GraphFileHeader.CHARSET));
  }

  @Test
  public void header() {
    assertArrayEquals(HEADER_BYTES, SUBJECT.header());
  }

  @Test
  public void magicNumber() {
    assertEquals("OpenTripPlannerGraph-01", SUBJECT.magicNumber());
  }

  @Test
  public void serializationId() {
    assertEquals("00000A", SUBJECT.serializationId());
  }

  @Test
  public void asString() {
    assertEquals("OpenTripPlannerGraph-01;00000A;", SUBJECT.asString());
  }

  @Test
  public void testToString() {
    assertEquals(
        "OpenTripPlannerGraph-01;00000A;",
        SUBJECT.toString()
    );
  }

  @Test
  public void padId() {
    assertEquals("00000A", GraphFileHeader.padId("A"));
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