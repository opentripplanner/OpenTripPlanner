package org.opentripplanner.model.plan.pagecursor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Base64;
import javax.annotation.Nullable;
import org.opentripplanner.framework.lang.StringUtils;
import org.opentripplanner.model.plan.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PageCursorSerializer {

  private static final int NOT_SET = Integer.MIN_VALUE;
  private static final byte VERSION = 1;
  private static final long TIME_ZERO = ZonedDateTime
    .of(2020, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC"))
    .toEpochSecond();
  private static final Logger LOG = LoggerFactory.getLogger(PageCursor.class);

  /** private constructor to prevent instantiating this utility class */
  private PageCursorSerializer() {
    /* empty */
  }

  @Nullable
  public static String encode(PageCursor cursor) {
    var buf = new ByteArrayOutputStream();
    try (var out = new ObjectOutputStream(buf)) {
      // The order must be the same in the encode and decode function
      writeByte(VERSION, out);
      writeEnum(cursor.type, out);
      writeTime(cursor.earliestDepartureTime, out);
      writeTime(cursor.latestArrivalTime, out);
      writeDuration(cursor.searchWindow, out);
      writeEnum(cursor.originalSortOrder, out);
      out.flush();
      return Base64.getUrlEncoder().encodeToString(buf.toByteArray());
    } catch (IOException e) {
      LOG.error("Failed to encode page cursor", e);
      return null;
    }
  }

  @Nullable
  public static PageCursor decode(String cursor) {
    if (StringUtils.hasNoValueOrNullAsString(cursor)) {
      return null;
    }
    try {
      var buf = Base64.getUrlDecoder().decode(cursor);
      var input = new ByteArrayInputStream(buf);

      var in = new ObjectInputStream(input);
      // The order must be the same in the encode and decode function

      // The version should be used to make serialization read/write forward and backward
      // compatible in the future.
      var version = readByte(in);
      var type = readEnum(in, PageType.class);
      var edt = readTime(in);
      var lat = readTime(in);
      var searchWindow = readDuration(in);
      var originalSortOrder = readEnum(in, SortOrder.class);

      return new PageCursor(type, originalSortOrder, edt, lat, searchWindow);
    } catch (Exception e) {
      String details = e.getMessage();
      if (details != null && !details.isBlank()) {
        LOG.warn("Unable to decode page cursor: '{}'. Details: {}", cursor, details);
      } else {
        LOG.warn("Unable to decode page cursor: '{}'.", cursor);
      }
      return null;
    }
  }

  private static void writeByte(byte value, ObjectOutputStream out) throws IOException {
    out.writeByte(value);
  }

  private static byte readByte(ObjectInputStream in) throws IOException {
    return in.readByte();
  }

  private static void writeTime(Instant time, ObjectOutputStream out) throws IOException {
    out.writeInt(time == null ? NOT_SET : (int) (time.getEpochSecond() - TIME_ZERO));
  }

  @Nullable
  private static Instant readTime(ObjectInputStream in) throws IOException {
    var value = in.readInt();
    return value == NOT_SET ? null : Instant.ofEpochSecond(TIME_ZERO + value);
  }

  private static void writeDuration(Duration duration, ObjectOutputStream out) throws IOException {
    out.writeInt((int) duration.toSeconds());
  }

  private static Duration readDuration(ObjectInputStream in) throws IOException {
    return Duration.ofSeconds(in.readInt());
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
