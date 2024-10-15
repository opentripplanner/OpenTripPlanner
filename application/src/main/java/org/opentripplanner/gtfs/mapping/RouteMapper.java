package org.opentripplanner.gtfs.mapping;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.opentripplanner.framework.collection.MapUtils;
import org.opentripplanner.framework.i18n.I18NString;
import org.opentripplanner.graph_builder.issue.api.DataImportIssueStore;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.network.GroupOfRoutes;
import org.opentripplanner.transit.model.network.Route;

/** Responsible for mapping GTFS Route into the OTP model. */
class RouteMapper {

  private final AgencyMapper agencyMapper;
  private final BrandingMapper brandingMapper;

  private final DataImportIssueStore issueStore;

  private TranslationHelper translationHelper;

  private final Map<org.onebusaway.gtfs.model.Route, Route> mappedRoutes = new HashMap<>();

  RouteMapper(
    AgencyMapper agencyMapper,
    DataImportIssueStore issueStore,
    TranslationHelper helper
  ) {
    this.agencyMapper = agencyMapper;
    this.issueStore = issueStore;
    this.brandingMapper = new BrandingMapper();
    this.translationHelper = helper;
  }

  Collection<Route> map(Collection<org.onebusaway.gtfs.model.Route> agencies) {
    return MapUtils.mapToList(agencies, this::map);
  }

  /** Map from GTFS to OTP model, {@code null} safe. */
  Route map(org.onebusaway.gtfs.model.Route orginal) {
    return orginal == null ? null : mappedRoutes.computeIfAbsent(orginal, this::doMap);
  }

  private Route doMap(org.onebusaway.gtfs.model.Route rhs) {
    var lhs = Route.of(AgencyAndIdMapper.mapAgencyAndId(rhs.getId()));
    I18NString longName = null;
    if (rhs.getLongName() != null) {
      longName =
        translationHelper.getTranslation(
          org.onebusaway.gtfs.model.Route.class,
          "longName",
          rhs.getId().getId(),
          rhs.getLongName()
        );
    }
    lhs.withAgency(agencyMapper.map(rhs.getAgency()));
    lhs.withShortName(rhs.getShortName());
    lhs.withLongName(longName);
    lhs.withGtfsType(rhs.getType());

    if (rhs.isSortOrderSet()) {
      lhs.withGtfsSortOrder(rhs.getSortOrder());
    }

    var mode = TransitModeMapper.mapMode(rhs.getType());

    if (mode == null) {
      issueStore.add(
        "RouteMapper",
        "Treating %s route type for route %s as BUS.",
        rhs.getType(),
        lhs.getId()
      );
      lhs.withMode(TransitMode.BUS);
    } else {
      lhs.withMode(mode);
    }
    lhs.withDescription(rhs.getDesc());
    lhs.withUrl(rhs.getUrl());
    lhs.withColor(rhs.getColor());
    lhs.withTextColor(rhs.getTextColor());
    lhs.withBikesAllowed(BikeAccessMapper.mapForRoute(rhs));
    lhs.withBranding(brandingMapper.map(rhs));
    if (rhs.getNetworkId() != null) {
      var networkId = GroupOfRoutes
        .of(new FeedScopedId(rhs.getId().getAgencyId(), rhs.getNetworkId()))
        .build();
      lhs.getGroupsOfRoutes().add(networkId);
    }

    return lhs.build();
  }
}
