/*
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by
 * the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *   https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 */

package org.opentripplanner.apis.transmodel.model.scalars;

import graphql.language.ArrayValue;
import graphql.language.FloatValue;
import graphql.schema.Coercing;
import graphql.schema.GraphQLScalarType;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;

public class GeoJSONCoordinatesScalar {

  private static final String DOCUMENTATION =
    "List of coordinates like: [[60.89, 11.12], [62.56, 12.10]]";
  private static final GraphQLScalarType INSTANCE = GraphQLScalarType.newScalar()
    .name("Coordinates")
    .description(DOCUMENTATION)
    .coercing(
      new Coercing() {
        @Override
        public List<List<Double>> serialize(Object input) {
          if (input instanceof Coordinate[]) {
            Coordinate[] coordinates = ((Coordinate[]) input);
            List<List<Double>> coordinateList = new ArrayList<>();
            for (Coordinate coordinate : coordinates) {
              List<Double> coordinatePair = new ArrayList<>();
              coordinatePair.add(coordinate.x);
              coordinatePair.add(coordinate.y);

              coordinateList.add(coordinatePair);
            }
            return coordinateList;
          }
          return null;
        }

        @Override
        public Coordinate[] parseValue(Object input) {
          List<List<? extends Number>> coordinateList = (List<List<? extends Number>>) input;

          Coordinate[] coordinates = new Coordinate[coordinateList.size()];

          for (int i = 0; i < coordinateList.size(); i++) {
            coordinates[i] = new Coordinate(
              coordinateList.get(i).get(0).doubleValue(),
              coordinateList.get(i).get(1).doubleValue()
            );
          }

          return coordinates;
        }

        @Override
        public Object parseLiteral(Object input) {
          if (input instanceof ArrayValue) {
            ArrayList<ArrayValue> coordinateList = (ArrayList) ((ArrayValue) input).getValues();
            Coordinate[] coordinates = new Coordinate[coordinateList.size()];

            for (int i = 0; i < coordinateList.size(); i++) {
              ArrayValue v = coordinateList.get(i);

              FloatValue longitude = (FloatValue) v.getValues().get(0);
              FloatValue latitude = (FloatValue) v.getValues().get(1);
              coordinates[i] = new Coordinate(
                longitude.getValue().doubleValue(),
                latitude.getValue().doubleValue()
              );
            }
            return coordinates;
          }
          return null;
        }
      }
    )
    .build();

  public static GraphQLScalarType getGraphQGeoJSONCoordinatesScalar() {
    return INSTANCE;
  }
}
