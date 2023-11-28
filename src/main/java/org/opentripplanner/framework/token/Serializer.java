package org.opentripplanner.framework.token;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.opentripplanner.framework.time.DurationUtils;

class Serializer implements Closeable {

  private final TokenDefinition definition;
  private final Object[] values;
  private final ObjectOutputStream out;
  private final ByteArrayOutputStream buf = new ByteArrayOutputStream();

  private Serializer(TokenDefinition definition, Object[] values) throws IOException {
    this.definition = definition;
    this.values = values;
    this.out = new ObjectOutputStream(buf);
  }

  @Override
  public void close() throws IOException {
    out.close();
  }

  static String serialize(TokenDefinition definition, Object[] values) throws IOException {
    try (var s = new Serializer(definition, values)) {
      s.writeInt(definition.version());

      for (var fieldName : definition.fieldNames()) {
        s.write(fieldName);
      }
      return s.serialize();
    }
  }

  private String serialize() throws IOException {
    out.close();
    return Base64.getUrlEncoder().encodeToString(buf.toByteArray());
  }

  private void write(String fieldName) throws IOException {
    write(fieldName, values[definition.index(fieldName)]);
  }

  private void write(String fieldName, Object value) throws IOException {
    var type = definition.type(fieldName);
    switch (type) {
      case BYTE -> writeByte((byte) value);
      case DURATION -> writeDuration((Duration) value);
      case INT -> writeInt((int) value);
      case STRING -> writeString((String) value);
      case TIME_INSTANT -> writeTimeInstant((Instant) value);
      default -> throw new IllegalArgumentException("Unknown type: " + type);
    }
  }

  private void writeByte(byte value) throws IOException {
    out.writeByte(value);
  }

  private void writeInt(int value) throws IOException {
    out.writeUTF(Integer.toString(value));
  }

  private void writeString(String value) throws IOException {
    out.writeUTF(value);
  }

  private void writeDuration(Duration duration) throws IOException {
    out.writeUTF(DurationUtils.durationToStr(duration));
  }

  private void writeTimeInstant(Instant time) throws IOException {
    out.writeUTF(time.toString());
  }
}
