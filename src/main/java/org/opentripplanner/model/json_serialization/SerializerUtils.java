package org.opentripplanner.model.json_serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.opentripplanner.common.geometry.GeometrySerializer;

public class SerializerUtils {

    public static SimpleModule getSerializerModule() {
        SimpleModule module = new SimpleModule("VertexJSONSerializer", new Version(1, 0, 0, null, "com.fasterxml.jackson.module", "jackson-module-jaxb-annotations"));
        module.addSerializer(new GeometrySerializer());
        module.addSerializer(new CoordinateSerializer());
        module.addSerializer(new PackedCoordinateSequenceSerializer());
        return module;
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = new ObjectMapper();
        AnnotationIntrospector aipair = new AnnotationIntrospectorPair (
            new JaxbAnnotationIntrospector(),
            new JacksonAnnotationIntrospector()
        );
        mapper.setAnnotationIntrospector(aipair);
        //REMOVE: mapper.configure(SerializationConfig.Feature.USE_ANNOTATIONS, true);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper;

    }
}
