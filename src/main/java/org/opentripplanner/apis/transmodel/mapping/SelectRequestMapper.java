package org.opentripplanner.apis.transmodel.mapping;

import static org.opentripplanner.apis.transmodel.mapping.TransitIdMapper.mapIDsToDomainNullSafe;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.opentripplanner.apis.transmodel.model.TransmodelTransportSubmode;
import org.opentripplanner.routing.api.request.request.filter.SelectRequest;
import org.opentripplanner.transit.model.basic.MainAndSubMode;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;

class SelectRequestMapper {

  @SuppressWarnings("unchecked")
  static SelectRequest mapSelectRequest(Map<String, List<?>> input) {
    var selectRequestBuilder = SelectRequest.of();

    if (input.containsKey("lines")) {
      var lines = (List<String>) input.get("lines");
      selectRequestBuilder.withRoutes(mapIDsToDomainNullSafe(lines));
    }

    if (input.containsKey("authorities")) {
      var authorities = (List<String>) input.get("authorities");
      selectRequestBuilder.withAgencies(mapIDsToDomainNullSafe(authorities));
    }

    if (input.containsKey("groupOfLines")) {
      var groupOfLines = (List<String>) input.get("groupOfLines");
      selectRequestBuilder.withGroupOfRoutes(mapIDsToDomainNullSafe(groupOfLines));
    }

    if (input.containsKey("transportModes")) {
      var tModes = new ArrayList<MainAndSubMode>();

      var transportModes = (List<Map<String, ?>>) input.get("transportModes");
      for (Map<String, ?> modeWithSubModes : transportModes) {
        var mainMode = (TransitMode) modeWithSubModes.get("transportMode");
        if (modeWithSubModes.containsKey("transportSubModes")) {
          var transportSubModes = (List<TransmodelTransportSubmode>) modeWithSubModes.get(
            "transportSubModes"
          );

          for (var subMode : transportSubModes) {
            tModes.add(new MainAndSubMode(mainMode, SubMode.of(subMode.getValue())));
          }
        } else {
          tModes.add(new MainAndSubMode(mainMode));
        }
      }
      selectRequestBuilder.withTransportModes(tModes);
    }

    return selectRequestBuilder.build();
  }
}
