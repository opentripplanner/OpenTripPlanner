package org.opentripplanner.framework.token;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.opentripplanner.framework.time.DurationUtils;

class Serializer {

  private final TokenDefinition definition;
  private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

  Serializer(TokenDefinition definition) {
    this.definition = definition;
  }

  String serialize(Object[] values) throws IOException {
    try (var out = new ObjectOutputStream(buf)) {
      writeInt(out, definition.version());

      for (var fieldName : definition.fieldNames()) {
        var value = values[definition.index(fieldName)];
        write(out, fieldName, value);
      }
      out.flush();
    }
    return Base64.getUrlEncoder().encodeToString(buf.toByteArray());
  }

  private void write(ObjectOutputStream out, String fieldName, Object value) throws IOException {
    var type = definition.type(fieldName);
    switch (type) {
      case BYTE -> writeByte(out, (byte) value);
      case DURATION -> writeDuration(out, (Duration) value);
      case INT -> writeInt(out, (int) value);
      case STRING -> writeString(out, (String) value);
      case TIME_INSTANT -> writeTimeInstant(out, (Instant) value);
      default -> throw new IllegalArgumentException("Unknown type: " + type);
    }
  }

  private static void writeByte(ObjectOutputStream out, byte value) throws IOException {
    out.writeByte(value);
  }

  private static void writeInt(ObjectOutputStream out, int value) throws IOException {
    out.writeUTF(Integer.toString(value));
  }

  private static void writeString(ObjectOutputStream out, String value) throws IOException {
    out.writeUTF(value);
  }

  private static void writeDuration(ObjectOutputStream out, Duration duration) throws IOException {
    out.writeUTF(DurationUtils.durationToStr(duration));
  }

  private static void writeTimeInstant(ObjectOutputStream out, Instant time) throws IOException {
    out.writeUTF(time.toString());
  }
}
