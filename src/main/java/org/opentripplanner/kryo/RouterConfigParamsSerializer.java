package org.opentripplanner.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.RouterConfigParams;

public class RouterConfigParamsSerializer extends Serializer<RouterConfigParams> {
    public static final String SOURCE = "SerializedGraph";

    @Override
    public void write(Kryo kryo, Output output, RouterConfigParams object) {
        output.writeString(object.toJson());
    }
    @Override
    public RouterConfigParams read(Kryo kryo, Input input, Class<RouterConfigParams> type) {
        return new RouterConfigParams(
                ConfigLoader.nodeFromString(input.readString(), SOURCE),
                SOURCE
        );
    }
}
