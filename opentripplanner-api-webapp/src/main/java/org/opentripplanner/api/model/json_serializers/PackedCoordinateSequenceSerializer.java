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
import org.codehaus.jackson.map.SerializerProvider;
import org.opentripplanner.common.geometry.PackedCoordinateSequence;

public class PackedCoordinateSequenceSerializer extends JsonSerializer<PackedCoordinateSequence> {
    int precision = 2;
    
    @Override
    public void serialize(PackedCoordinateSequence value, JsonGenerator jgen,
            SerializerProvider provider) throws IOException, JsonProcessingException {
        jgen.writeStartArray();
        for (int i = 0; i < value.size(); ++i) {
            jgen.writeStartArray();
            for (int j = 0; j < value.getDimension(); j++) {
                double ordinate = value.getOrdinate(i, j);
                if (!Double.isNaN(ordinate)) {
                    if (precision == -1) {
                        jgen.writeObject(ordinate);
                    } else {
                        jgen.writeRawValue(String.format("%." + precision + "f", ordinate));
                    }
                }
            }
            jgen.writeEndArray();
        }
        jgen.writeEndArray();
    }
    
    @Override
    public Class<PackedCoordinateSequence> handledType() {
        return PackedCoordinateSequence.class;
    }

}
