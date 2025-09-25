package org.opentripplanner.model.plan.legreference;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

  /** private constructor to prevent instantiating this utility class */
  private LegReferenceSerializer() {}

  @Nullable
  public static String encode(LegReference legReference) {
    if (legReference == null) {
      return null;
    }
    LegReferenceType typeEnum = LegReferenceType.forClass(legReference.getClass()).orElseThrow(() ->
      new IllegalArgumentException("Unknown LegReference type")
    );

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

    byte[] serializedLegReference;
    try {
      serializedLegReference = Base64.getUrlDecoder().decode(legReference);
    } catch (IllegalArgumentException e) {
      LOG.info("Unable to decode leg reference (invalid base64 encoding): '{}'", legReference, e);
      return null;
    }
    var input = new ByteArrayInputStream(serializedLegReference);

    try (var in = new ObjectInputStream(input)) {
      // The order must be the same in the encode and decode function

      var type = readEnum(in, LegReferenceType.class);
      return type.getDeserializer().read(in);
    } catch (IOException e) {
      LOG.warn(
        "Unable to decode leg reference (incompatible serialization format): '{}'",
        legReference,
        e
      );
      return null;
    }
  }

  static void writeScheduledTransitLegV3(LegReference ref, ObjectOutputStream out)
    throws IOException {
    if (ref instanceof ScheduledTransitLegReference s) {
      out.writeUTF(s.tripOnServiceDateId() == null ? s.tripId().toString() : "");
      out.writeUTF(s.serviceDate().toString());
      out.writeInt(s.fromStopPositionInPattern());
      out.writeInt(s.toStopPositionInPattern());
      out.writeUTF(s.fromStopId().toString());
      out.writeUTF(s.toStopId().toString());
      out.writeUTF(s.tripOnServiceDateId() == null ? "" : s.tripOnServiceDateId().toString());
    } else {
      throw new IllegalArgumentException("Invalid LegReference type");
    }
  }

  static LegReference readScheduledTransitLegV3(ObjectInputStream objectInputStream)
    throws IOException {
    return new ScheduledTransitLegReference(
      FeedScopedId.parse(objectInputStream.readUTF()),
      LocalDate.parse(objectInputStream.readUTF(), DateTimeFormatter.ISO_LOCAL_DATE),
      objectInputStream.readInt(),
      objectInputStream.readInt(),
      FeedScopedId.parse(objectInputStream.readUTF()),
      FeedScopedId.parse(objectInputStream.readUTF()),
      FeedScopedId.parse(objectInputStream.readUTF())
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
