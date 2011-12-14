package org.opentripplanner.api.model.json_serializers;

import org.codehaus.jackson.Version;
import org.codehaus.jackson.map.AnnotationIntrospector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializationConfig;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;
import org.codehaus.jackson.map.introspect.JacksonAnnotationIntrospector;
import org.codehaus.jackson.map.module.SimpleModule;
import org.codehaus.jackson.xc.JaxbAnnotationIntrospector;

public class SerializerUtils {

    public static SimpleModule getSerializerModule() {
        SimpleModule module = new SimpleModule("VertexJSONSerializer", new Version(1, 0, 0, null));
        module.addSerializer(new GeoJSONSerializer());
        module.addSerializer(new CoordinateSerializer());
        module.addSerializer(new PackedCoordinateSequenceSerializer());
        return module;
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();

        AnnotationIntrospector primary = new JacksonAnnotationIntrospector();
        AnnotationIntrospector secondary = new JaxbAnnotationIntrospector();
        AnnotationIntrospector pair = new AnnotationIntrospector.Pair(primary, secondary);
        mapper.setAnnotationIntrospector(pair);
        mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
        mapper.getSerializationConfig().withSerializationInclusion(Inclusion.NON_NULL);
        return mapper;

    }
}
