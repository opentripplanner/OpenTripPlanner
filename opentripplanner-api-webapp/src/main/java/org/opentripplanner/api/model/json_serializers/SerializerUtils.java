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
