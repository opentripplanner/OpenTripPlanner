package org.opentripplanner.kryo;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.esotericsoftware.kryo.serializers.JavaSerializer;
import java.util.HashSet;
import java.util.Set;

/**
 * Java ImmutableCollections are serialized by default using {@link java.util.CollSer}, which is not
 * compatible with Kryo 4. Until we move to Kryo 5, we need to have this in place.
 */
public class JdkImmutableSetSerializer extends JavaSerializer {

    @Override
    public void write(Kryo kryo, Output output, Object object) {
        Set copy = new HashSet((Set) object);
        super.write(kryo, output, copy);
    }

    @Override
    public Set<Object> read(Kryo kryo, Input input, Class type) {
        Set copy = (Set) super.read(kryo, input, type);
        return Set.copyOf(copy);
    }
}
