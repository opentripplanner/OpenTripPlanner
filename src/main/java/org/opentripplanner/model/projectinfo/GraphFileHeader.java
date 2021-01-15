package org.opentripplanner.model.projectinfo;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * The Graph.obj file start with file header. The header have two things:
 * <ol>
 *   <li>Magic number: {@code "OpenTripPlannerGraph-01"}</li>
 *   <li>The graph file serialization compatibility id</li>
 * </ol>
 *
 * This class represent the header and contain logic to parse and validate it.
 */
public class GraphFileHeader {

  private static final String MAGIC_NUMBER = "OpenTripPlannerGraph-01";
  private static final char ID_PREFIX = '0';
  private static final char DELIMITER = ';';
  private static final int SER_ID_LENGTH = 6;
  private static final int HEADER_LENGTH = MAGIC_NUMBER.length() + SER_ID_LENGTH + 2;

  private static final Pattern HEADER_PATTERN = Pattern.compile(
      MAGIC_NUMBER + DELIMITER + "([-\\w/+:]{6})" + DELIMITER
  );
  public static final Charset CHARSET = StandardCharsets.ISO_8859_1;
  public static final String UNKNOWN_ID = "------";

  private final String serializationId;
  private final byte[] bytes;

  GraphFileHeader() {
    this(UNKNOWN_ID);
  }

  public GraphFileHeader(String serializationId) {
    this.serializationId = padId(serializationId);
    this.bytes = toString().getBytes(CHARSET);
  }

  public static int headerLength() {
    return HEADER_LENGTH;
  }

  public static GraphFileHeader parse(byte[] buf) {
    if(buf.length < HEADER_LENGTH) {
      throw new IllegalArgumentException(
          "Input file header is not large enough. At least " + HEADER_LENGTH + " bytes is needed. "
              + "Input: " + prettyBytesToString(buf)
      );
    }
    String header = new String(buf, 0, HEADER_LENGTH, CHARSET);

    Matcher m = HEADER_PATTERN.matcher(header);
    if(!m.matches()) {
      throw new IllegalArgumentException(
          "The file is no recognized as an OTP Graph file. The header do not match \""
              + HEADER_PATTERN.pattern() + "\". Input: " + prettyBytesToString(buf)
      );
    }
    return new GraphFileHeader(m.group(1));
  }

  public byte[] header() {
    return bytes;
  }

  public String magicNumber() {
    return MAGIC_NUMBER;
  }

  /**
   * The graph file serialization compatibility id. If OTP and a Graph.obj file have the same
   * version, then OTP is compatible with the graph and should be able to deserialize it.
   */
  public String serializationId() {
    return serializationId;
  }

  public String asString() {
    return MAGIC_NUMBER + DELIMITER + serializationId + DELIMITER;
  }

  public boolean isUnknown() {
    return UNKNOWN_ID.equals(serializationId);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }
    GraphFileHeader that = (GraphFileHeader) o;
    return serializationId.equals(that.serializationId);
  }

  @Override
  public int hashCode() {
    return serializationId.hashCode();
  }

  @Override
  public String toString() {
    return asString();
  }

  static String padId(String text) {
    StringBuilder buf = new StringBuilder();
    while (buf.length() + text.length() < SER_ID_LENGTH) { buf.append(ID_PREFIX); }
    buf.append(text);
    return buf.toString();
  }

  /** Example: 41 6C 66 61 2D 31  "Alfa-1" */
  static String prettyBytesToString(byte[] text) {
    if(text == null || text.length == 0) {
      return "<empty>";
    }
    StringBuilder buf = new StringBuilder();
    for (int v : text) {
      buf.append(" ").append(String.format("%02X", v));
    }
    return buf.substring(1) + "  \"" + new String(text, CHARSET) + "\"";
  }
}
