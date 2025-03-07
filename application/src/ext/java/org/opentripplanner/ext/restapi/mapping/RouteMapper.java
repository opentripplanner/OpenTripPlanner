package org.opentripplanner.ext.restapi.mapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.restapi.model.ApiRoute;
import org.opentripplanner.ext.restapi.model.ApiRouteShort;
import org.opentripplanner.framework.i18n.I18NStringMapper;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.organization.Branding;

public class RouteMapper {

  public static List<ApiRoute> mapToApi(Collection<Route> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(RouteMapper::mapToApi).collect(Collectors.toList());
  }

  public static ApiRoute mapToApi(Route domain) {
    if (domain == null) {
      return null;
    }
    I18NStringMapper stringMapper = new I18NStringMapper(null);
    ApiRoute api = new ApiRoute();
    api.id = FeedScopedIdMapper.mapToApi(domain.getId());
    api.agency = AgencyMapper.mapToApi(domain.getAgency());
    api.shortName = domain.getShortName();
    api.longName = stringMapper.mapToApi(domain.getLongName());
    api.mode = ModeMapper.mapToApi(domain.getMode());
    api.type = domain.getGtfsType() != null
      ? domain.getGtfsType()
      : RouteTypeMapper.mapToApi(domain.getMode());
    api.desc = domain.getDescription();
    api.url = domain.getUrl();
    api.color = domain.getColor();
    api.textColor = domain.getTextColor();
    api.bikesAllowed = BikeAccessMapper.mapToApi(domain.getBikesAllowed());
    api.sortOrder = domain.getGtfsSortOrder();

    Branding branding = domain.getBranding();
    if (branding != null) {
      api.brandingUrl = branding.getUrl();
    }

    return api;
  }

  public static List<ApiRouteShort> mapToApiShort(Collection<Route> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(RouteMapper::mapToApiShort).collect(Collectors.toList());
  }

  public static ApiRouteShort mapToApiShort(Route domain) {
    if (domain == null) {
      return null;
    }
    I18NStringMapper stringMapper = new I18NStringMapper(null);
    ApiRouteShort api = new ApiRouteShort();
    api.id = FeedScopedIdMapper.mapToApi(domain.getId());
    api.shortName = domain.getShortName();
    api.longName = stringMapper.mapToApi(domain.getLongName());
    api.mode = ModeMapper.mapToApi(domain.getMode());
    api.color = domain.getColor();
    api.agencyName = domain.getAgency().getName();

    return api;
  }
}
