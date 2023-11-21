package org.opentripplanner.framework.token;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import org.opentripplanner.framework.time.DurationUtils;

class Deserializer {

  private final ByteArrayInputStream input;

  Deserializer(String token) {
    this.input = new ByteArrayInputStream(Base64.getUrlDecoder().decode(token));
  }

  List<Object> deserialize(TokenDefinition definition) throws IOException {
    try {
      // Assume deprecated fields are included in the token
      return readFields(definition, false);
    } catch (IOException ignore) {
      // If the token is the next version, then deprecated field are removed. Try
      // skipping the deprecated tokens
      return readFields(definition, true);
    }
  }

  private List<Object> readFields(TokenDefinition definition, boolean matchNewVersionPlusOne)
    throws IOException {
    input.reset();
    List<Object> result = new ArrayList<>();

    var in = new ObjectInputStream(input);

    readAndMatchVersion(in, definition, matchNewVersionPlusOne);

    for (FieldDefinition field : definition.listFields()) {
      if (matchNewVersionPlusOne && field.deprecated()) {
        continue;
      }
      var v = read(in, field);
      if (!field.deprecated()) {
        result.add(v);
      }
    }
    return result;
  }

  private void readAndMatchVersion(
    ObjectInputStream in,
    TokenDefinition definition,
    boolean matchVersionPlusOne
  ) throws IOException {
    int matchVersion = (matchVersionPlusOne ? 1 : 0) + definition.version();

    int v = readInt(in);
    if (v != matchVersion) {
      throw new IOException(
        "Version does not match. Token version: " + v + ", schema version: " + definition.version()
      );
    }
  }

  private Object read(ObjectInputStream in, FieldDefinition field) throws IOException {
    return switch (field.type()) {
      case BYTE -> readByte(in);
      case DURATION -> readDuration(in);
      case INT -> readInt(in);
      case STRING -> readString(in);
      case TIME_INSTANT -> readTimeInstant(in);
    };
  }

  private static byte readByte(ObjectInputStream in) throws IOException {
    return in.readByte();
  }

  private static int readInt(ObjectInputStream in) throws IOException {
    return Integer.parseInt(in.readUTF());
  }

  private static String readString(ObjectInputStream in) throws IOException {
    return in.readUTF();
  }

  private static Duration readDuration(ObjectInputStream in) throws IOException {
    return DurationUtils.duration(in.readUTF());
  }

  private static Instant readTimeInstant(ObjectInputStream in) throws IOException {
    return Instant.parse(in.readUTF());
  }
}
