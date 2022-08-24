package org.opentripplanner.model.plan.legreference;

import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.YEAR;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Base64;
import javax.annotation.Nullable;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Serializer for LegReferences
 */
public class LegReferenceSerializer {

  private static final Logger LOG = LoggerFactory.getLogger(LegReferenceSerializer.class);

  // TODO: This is for backwards compatibility. Change to use ISO_LOCAL_DATE after OTP v2.2 is released
  private static final DateTimeFormatter LENIENT_ISO_LOCAL_DATE = new DateTimeFormatterBuilder()
    .appendValue(YEAR, 4)
    .optionalStart()
    .appendLiteral('-')
    .optionalEnd()
    .appendValue(MONTH_OF_YEAR, 2)
    .optionalStart()
    .appendLiteral('-')
    .optionalEnd()
    .appendValue(DAY_OF_MONTH, 2)
    .toFormatter();

  /** private constructor to prevent instantiating this utility class */
  private LegReferenceSerializer() {}

  @Nullable
  public static String encode(LegReference legReference) {
    if (legReference == null) {
      return null;
    }
    LegReferenceType typeEnum = LegReferenceType.forClass(legReference.getClass());

    if (typeEnum == null) {
      throw new IllegalArgumentException("Unknown LegReference type");
    }

    var buf = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(buf)) {
      // The order must be the same in the encode and decode function
      writeEnum(typeEnum, out);
      typeEnum.getSerializer().write(legReference, out);
      out.flush();
      return Base64.getUrlEncoder().encodeToString(buf.toByteArray());
    } catch (IOException e) {
      LOG.error("Failed to encode leg reference", e);
      return null;
    }
  }

  @Nullable
  public static LegReference decode(String legReference) {
    if (legReference == null) {
      return null;
    }

    var buf = Base64.getUrlDecoder().decode(legReference);
    var input = new ByteArrayInputStream(buf);

    try (var in = new ObjectInputStream(input)) {
      // The order must be the same in the encode and decode function

      var type = readEnum(in, LegReferenceType.class);
      return type.getDeserializer().read(in);
    } catch (IOException | ParseException e) {
      LOG.error("Unable to decode leg reference: '" + legReference + "'", e);
      return null;
    }
  }

  static void writeScheduledTransitLeg(LegReference ref, ObjectOutputStream out)
    throws IOException {
    if (ref instanceof ScheduledTransitLegReference s) {
      out.writeUTF(s.tripId().toString());
      out.writeUTF(s.serviceDate().toString());
      out.writeInt(s.fromStopPositionInPattern());
      out.writeInt(s.toStopPositionInPattern());
    } else {
      throw new IllegalArgumentException("Invalid LegReference type");
    }
  }

  static LegReference readScheduledTransitLeg(ObjectInputStream objectInputStream)
    throws IOException {
    return new ScheduledTransitLegReference(
      FeedScopedId.parseId(objectInputStream.readUTF()),
      LocalDate.parse(objectInputStream.readUTF(), LENIENT_ISO_LOCAL_DATE),
      objectInputStream.readInt(),
      objectInputStream.readInt()
    );
  }

  private static <T extends Enum<T>> void writeEnum(T value, ObjectOutputStream out)
    throws IOException {
    out.writeUTF(value.name());
  }

  @SuppressWarnings("SameParameterValue")
  private static <T extends Enum<T>> T readEnum(ObjectInputStream in, Class<T> enumType)
    throws IOException {
    String value = in.readUTF();
    return Enum.valueOf(enumType, value);
  }
}
