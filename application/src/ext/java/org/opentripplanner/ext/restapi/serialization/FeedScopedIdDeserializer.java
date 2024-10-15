package org.opentripplanner.ext.restapi.serialization;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FeedScopedIdDeserializer extends JsonDeserializer<FeedScopedId> {

  public static final String SEPARATOR = ":";

  @Override
  public FeedScopedId deserialize(JsonParser jp, DeserializationContext ctxt)
    throws IOException, JsonProcessingException {
    String string = jp.getValueAsString();
    String[] parts = string.split(SEPARATOR, 2);
    return new FeedScopedId(parts[0], parts[1]);
  }

  // Gets around type erasure, allowing
  // module.addSerializer(new ThingSerializer()) to correctly associate
  // this serializer with the proper type.
  @Override
  public Class<FeedScopedId> handledType() {
    return FeedScopedId.class;
  }
}
