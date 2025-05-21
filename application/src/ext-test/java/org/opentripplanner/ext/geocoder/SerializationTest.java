package org.opentripplanner.ext.geocoder;

import static org.opentripplanner.ext.geocoder.StopCluster.LocationType.STOP;
import static org.opentripplanner.test.support.JsonAssertions.assertEqualJson;
import static org.opentripplanner.transit.model._data.TimetableRepositoryForTest.id;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.json.ObjectMappers;

class SerializationTest {

  private static final String STOP_CLUSTER_JSON =
    """
      {
        "primary" : {
          "id" : "F:123",
          "code" : "aaa",
          "type" : "STOP",
          "name" : "A stop",
          "coordinate" : {
            "lat" : 1.0,
            "lon" : 2.0
          },
          "modes" : [
            "RAIL"
          ],
          "agencies" : [
            {
              "id" : "F:a1",
              "name" : "Agency"
            }
          ],
          "feedPublisher" : {
            "name" : "Publisher"
          }
        },
        "secondaries" : [
          {
            "id" : "F:123",
            "code" : "aaa",
            "type" : "STOP",
            "name" : "A stop",
            "coordinate" : {
              "lat" : 1.0,
              "lon" : 2.0
            },
            "modes" : [
              "RAIL"
            ],
            "agencies" : [
              {
                "id" : "F:a1",
                "name" : "Agency"
              }
            ],
            "feedPublisher" : {
              "name" : "Publisher"
            }
          }
        ]
      }
    """;

  @Test
  void serialize() {
    var mapper = ObjectMappers.ignoringExtraFields();

    var loc = new StopCluster.Location(
      id("123"),
      "aaa",
      STOP,
      "A stop",
      new StopCluster.Coordinate(1.0d, 2.0d),
      Set.of("RAIL"),
      List.of(new StopCluster.Agency(id("a1"), "Agency")),
      new StopCluster.FeedPublisher("Publisher")
    );
    var cluster = new StopCluster(loc, List.of(loc));

    var json = mapper.valueToTree(cluster);

    assertEqualJson(STOP_CLUSTER_JSON, json);
  }
}
