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

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializer;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.module.SimpleModule;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;


/**
 * This serializes an object containing Vertex objects, replacing any edges with integer edge ids.
 * @see EdgeJSONSerializer
 * @author novalis
 *
 */
public class VertexSetJSONSerializer extends JsonSerializer<WithGraph> {

    @Override
    public void serialize(WithGraph object, JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        // FIXME: there is probably a simple way to automatically wire this up that I can't find
        // right now 
        ObjectMapper mapper = SerializerUtils.getMapper();

        SimpleModule module = SerializerUtils.getSerializerModule();
        module.addSerializer(new EdgeSerializer(object.getGraph()));
        
        mapper.registerModule(module);
        //configuring jgen to just use the mapper doesn't actually work
        jgen.writeRawValue(mapper.writeValueAsString(object.getObject()));
    }
    
    class EdgeSerializer extends JsonSerializer<Edge> {

        private Graph graph;

        public EdgeSerializer(Graph graph) {
            this.graph = graph;
        }

        @Override
        public void serialize(Edge value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException, JsonProcessingException {
            Integer edgeId = graph.getIdForEdge(value);
            jgen.writeObject(edgeId);
        }
        
        @Override
        public Class<Edge> handledType() {
            return Edge.class;
        }
        
    }

    /*
     * // a vertex is serialized as a set of fields, including a superclass field pointing to //
     * fields from its superclass if (object instanceof List<?>) { jgen.writeStartArray(); for
     * (Object element : ((List<?>) object)) { serialize(element, jgen, provider); }
     * jgen.writeEndArray(); return; }
     * 
     * int openObjects = 1;
     * 
     * Class<?> c = object.getClass(); Field[] fields; while (c != null && c != Object.class) {
     * jgen.writeStartObject();
     * 
     * openObjects += 1; fields = c.getDeclaredFields(); for (int i = 0; i < fields.length; i++) {
     * Field field = fields[i]; int modifiers = field.getModifiers(); if ((modifiers &
     * Modifier.STATIC) != 0) { continue; } field.setAccessible(true); String name =
     * field.getName(); Object value = "(unknown)"; try { value = field.get(object); } catch
     * (IllegalArgumentException e1) { e1.printStackTrace(); } catch (IllegalAccessException e1) {
     * e1.printStackTrace(); } jgen.writeFieldName(name); writeValue(jgen, value); } c =
     * c.getSuperclass(); if (c != null && c != Object.class) { jgen.writeFieldName("superclass");
     * ++openObjects; } }
     * 
     * for (int i = 0; i < openObjects; ++i) { jgen.writeEndObject(); } }
     * 
     * private void writeValue(JsonGenerator jgen, Object value) throws JsonGenerationException,
     * IOException {
     * 
     * if (value instanceof Geometry) { GeometryJSON json = new GeometryJSON();
     * jgen.writeRawValue(json.toString((Geometry) value)); } else { if (value instanceof Edge) { //
     * FIXME: need to get the edge ID from the graph value = "removed edge (for now)"; } else if
     * (value instanceof List) { jgen.writeStartArray(); for (Object o: (List<?>) value) {
     * writeValue(jgen, value); } jgen.writeEndArray(); } else if (value instanceof Number || value
     * instanceof String || value instanceof Boolean) { jgen.writeObject(value); } else { //compound
     * object, need to deconstruct it ourselves... this is nuts } } }
     */
}
