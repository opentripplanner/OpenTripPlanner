package org.opentripplanner.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.RouterConfig;

/**
 * This serializer is needed because there is no default constructor on the {@link RouterConfig}.
 * The router config is created passing in a JSON node. It save this internally and this is used to
 * serialize the router config. We serialize the JsonNode as a String instead of the graph router
 * POJO it self. This make it easy to deserialize it by using the constructor.
 * <p>
 * We serialize the JsonNode, not the original JSON String, because we do not want the environment
 * variables to be resolved twice. They are resolved for the raw JSON string when it is read from
 * the file system, then the string is mapped to a JsonNode. Also, the JSON graph serialized string
 * is normalized without comments, and extra whitespace.
 */
public class RouterConfigSerializer extends Serializer<RouterConfig> {
    public static final String SOURCE = "SerializedGraph";

    @Override
    public void write(Kryo kryo, Output output, RouterConfig object) {
        output.writeString(object.toJson());
    }
    @Override
    public RouterConfig read(Kryo kryo, Input input, Class<RouterConfig> type) {
        return new RouterConfig(
                ConfigLoader.nodeFromString(input.readString(), SOURCE),
                SOURCE,
                false
        );
    }
}
