package org.opentripplanner.model.projectinfo;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.opentripplanner.framework.application.OtpAppException;

/**
 * The Graph.obj file start with file header. The header have two things:
 * <ol>
 *   <li>Magic number: {@code "OpenTripPlannerGraph"}</li>
 *   <li>The graph file serialization compatibility id</li>
 * </ol>
 * <p>
 * This class represent the header and contain logic to parse and validate it.
 */
public class GraphFileHeader implements Serializable {

  private static final String MAGIC_NUMBER = "OpenTripPlannerGraph";
  private static final char ID_PREFIX = '0';
  private static final char DELIMITER = ';';
  private static final int ID_LENGTH = 7;
  private static final int HEADER_LENGTH = MAGIC_NUMBER.length() + ID_LENGTH + 2;

  private static final Pattern HEADER_PATTERN = Pattern.compile(
    MAGIC_NUMBER + DELIMITER + "([-\\w.:+/]{7})" + DELIMITER
  );

  public static final Charset CHARSET = StandardCharsets.ISO_8859_1;
  public static final String UNKNOWN_ID = "UNKNOWN";

  private final String otpSerializationVersionId;
  private final byte[] bytes;

  GraphFileHeader() {
    this(UNKNOWN_ID);
  }

  GraphFileHeader(String otpSerializationVersionId) {
    this.otpSerializationVersionId = stripId(otpSerializationVersionId);
    this.bytes = asString().getBytes(CHARSET);
  }

  public static int headerLength() {
    return HEADER_LENGTH;
  }

  public static GraphFileHeader parse(byte[] buf) {
    if (buf.length < HEADER_LENGTH) {
      throw new OtpAppException(
        "Input file header is not large enough. At least " +
        HEADER_LENGTH +
        " bytes is needed. " +
        "Input: " +
        prettyBytesToString(buf)
      );
    }
    String header = new String(buf, 0, HEADER_LENGTH, CHARSET);

    Matcher m = HEADER_PATTERN.matcher(header);
    if (!m.matches()) {
      throw new OtpAppException(
        "The file is no recognized as an OTP Graph file. The header do not match \"" +
        HEADER_PATTERN.pattern() +
        "\".\n\tInput: " +
        prettyBytesToString(buf)
      );
    }
    return new GraphFileHeader(m.group(1));
  }

  /** Return the entire header including magic-number and version id as a byte array */
  public byte[] header() {
    return bytes;
  }

  public String magicNumber() {
    return MAGIC_NUMBER;
  }

  /**
   * The OTP serialization version id. If OTP and a Graph.obj file have the same id, then OTP is
   * compatible with the graph and should be able to deserialize it.
   * <p>
   * The returned id do NOT include the prefix '0's. In the serialized form as bytes it have a fixed
   * length {@link #ID_LENGTH} and is padded with {@link #ID_PREFIX}.
   */
  public String otpSerializationVersionId() {
    return otpSerializationVersionId;
  }

  public String otpSerializationVersionIdPadded() {
    return padId(otpSerializationVersionId);
  }

  public String asString() {
    return MAGIC_NUMBER + DELIMITER + otpSerializationVersionIdPadded() + DELIMITER;
  }

  public boolean isUnknown() {
    return UNKNOWN_ID.equals(otpSerializationVersionId);
  }

  @Override
  public int hashCode() {
    return otpSerializationVersionId.hashCode();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GraphFileHeader that = (GraphFileHeader) o;
    return otpSerializationVersionId.equals(that.otpSerializationVersionId);
  }

  @Override
  public String toString() {
    return asString();
  }

  /**
   * Pad the given text until it have the expected length {@link #ID_LENGTH} using the {@link
   * #ID_PREFIX}.
   */
  static String padId(String text) {
    StringBuilder buf = new StringBuilder();
    while (buf.length() + text.length() < ID_LENGTH) {
      buf.append(ID_PREFIX);
    }
    buf.append(text);
    return buf.toString();
  }

  /**
   * Strip of  any {@link #ID_PREFIX} characters form the beginning of the given text.
   */
  static String stripId(String text) {
    int pos = 0;
    while (pos < text.length() && text.charAt(pos) == ID_PREFIX) {
      ++pos;
    }
    return text.substring(pos);
  }

  /** Example: 41 6C 66 61 2D 31  "Alfa-1" */
  static String prettyBytesToString(byte[] text) {
    if (text == null || text.length == 0) {
      return "[empty]";
    }
    StringBuilder buf = new StringBuilder();
    for (int v : text) {
      buf.append(" ").append(String.format("%02X", v));
    }
    return buf.substring(1) + "  \"" + new String(text, CHARSET) + "\"";
  }
}
