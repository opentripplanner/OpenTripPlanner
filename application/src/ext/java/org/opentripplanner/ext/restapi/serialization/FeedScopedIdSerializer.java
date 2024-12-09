package org.opentripplanner.ext.restapi.serialization;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import java.io.IOException;
import org.opentripplanner.transit.model.framework.FeedScopedId;

public class FeedScopedIdSerializer extends JsonSerializer<FeedScopedId> {

  public static final String SEPARATOR = ":";

  /** This creates a Jackson module including both the serializer and deserializer for AgencyAndIds. */
  public static SimpleModule makeModule() {
    Version moduleVersion = new Version(1, 0, 0, null, null, null);
    SimpleModule module = new SimpleModule("OTP", moduleVersion);
    module.addSerializer(FeedScopedId.class, new FeedScopedIdSerializer());
    module.addDeserializer(FeedScopedId.class, new FeedScopedIdDeserializer());
    // Map key (de)serializers are always separate from value ones, because they must be strings.
    module.addKeyDeserializer(FeedScopedId.class, new FeedScopedIdKeyDeserializer());
    return module;
  }

  @Override
  public void serialize(FeedScopedId a, JsonGenerator gen, SerializerProvider prov)
    throws IOException, JsonProcessingException {
    gen.writeString(a.getFeedId() + SEPARATOR + a.getId());
  }

  // Gets around type erasure, allowing
  // module.addSerializer(new ThingSerializer()) to correctly associate
  // this serializer with the proper type.
  @Override
  public Class<FeedScopedId> handledType() {
    return FeedScopedId.class;
  }
}
