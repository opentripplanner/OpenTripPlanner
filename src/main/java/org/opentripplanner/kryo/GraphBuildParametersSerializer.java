package org.opentripplanner.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import org.opentripplanner.standalone.config.ConfigLoader;
import org.opentripplanner.standalone.config.GraphBuildParameters;

public class GraphBuildParametersSerializer extends Serializer<GraphBuildParameters> {
    public static final String SOURCE = "SerializedGraph";

    @Override
    public void write(Kryo kryo, Output output, GraphBuildParameters object) {
        output.writeString(object.toJson());
    }

    @Override
    public GraphBuildParameters read(Kryo kryo, Input input, Class<GraphBuildParameters> type) {
        return new GraphBuildParameters(
                ConfigLoader.nodeFromString(input.readString(), SOURCE),
                SOURCE
        );
    }
}