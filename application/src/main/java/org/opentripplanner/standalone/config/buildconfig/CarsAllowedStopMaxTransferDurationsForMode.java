package org.opentripplanner.standalone.config.buildconfig;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_7;

import java.time.Duration;
import java.util.Map;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.api.request.framework.DurationForEnum;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class CarsAllowedStopMaxTransferDurationsForMode {

  public static DurationForEnum<StreetMode> map(
    NodeAdapter root,
    String parameterName,
    Duration maxTransferDuration
  ) {
    Map<StreetMode, Duration> values = root
      .of(parameterName)
      .since(V2_7)
      .summary(
        "This is used for specifying a `maxTransferDuration` value for bikes and cars to use with transfers between stops that have trips with cars."
      )
      .description(
        """
This is a special parameter that only works on transfers between stops that have trips that allow cars.
The duration can be set for either 'BIKE' or 'CAR'.
For cars, transfers are only calculated between stops that have trips that allow cars.
For cars, this overrides the default `maxTransferDuration`.
For bicycles, this indicates that additional transfers should be calculated with the specified duration between stops that have trips that allow cars.

**Example**

```JSON
// build-config.json
{
  "carsAllowedStopMaxTransferDurationsForMode": {
    "CAR": "2h",
    "BIKE": "3h"
  } 
}
```
"""
      )
      .asEnumMap(StreetMode.class, Duration.class);
    for (StreetMode mode : values.keySet()) {
      if (mode != StreetMode.BIKE && mode != StreetMode.CAR) {
        throw new IllegalArgumentException(
          "Only the CAR and BIKE modes are allowed in the carsAllowedStopMaxTransferDurationsForMode parameter."
        );
      }
    }
    return DurationForEnum.of(StreetMode.class).withValues(values).build();
  }
}
