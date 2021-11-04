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

import java.io.IOException;
import java.util.Date;


import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.opentripplanner.graph_builder.model.GraphVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Deserialize the version information about the graph and load into memory.
 */
public class GraphVersionDeserializer extends JsonDeserializer<GraphVersion> {

  private static final Logger LOG = LoggerFactory.getLogger(GraphVersionDeserializer.class);
  @Override
  public GraphVersion deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
    GraphVersion gi = new GraphVersion();
    while (!jsonParser.isClosed()) {
      JsonToken token = jsonParser.nextToken();
      if (token != null) {
        if ("created".equals(jsonParser.getCurrentName())) {
          token = jsonParser.nextToken();
          gi.setCreatedDate(new Date(jsonParser.getValueAsLong()));
        } else if ("version".equals(jsonParser.getCurrentName())) {
          token = jsonParser.nextToken();
          gi.setVersion(jsonParser.getValueAsString());
        } else {
          LOG.info("unexpected token " + jsonParser.getCurrentName() + ":" + jsonParser.getCurrentValue());
        }
      }
    }
    return gi;
  }

}
