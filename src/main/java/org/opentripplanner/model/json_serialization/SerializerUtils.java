/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.model.json_serialization;

import com.conveyal.geojson.GeometrySerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotationIntrospectorPair;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;

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
