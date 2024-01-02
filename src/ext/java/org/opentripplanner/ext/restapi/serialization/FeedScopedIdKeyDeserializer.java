package org.opentripplanner.ext.restapi.serialization;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.KeyDeserializer;
import java.io.IOException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

// Map key (de)serializers are always separate from value ones, because they must be strings.
public class FeedScopedIdKeyDeserializer extends KeyDeserializer {

  public static final String SEPARATOR = ":";

  @Override
  public Object deserializeKey(String key, DeserializationContext ctxt) throws IOException {
    String[] parts = key.split(SEPARATOR, 2);
    return new FeedScopedId(parts[0], parts[1]);
  }
}
