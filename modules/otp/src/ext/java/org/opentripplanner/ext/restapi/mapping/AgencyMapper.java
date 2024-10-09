package org.opentripplanner.ext.restapi.mapping;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.restapi.model.ApiAgency;
import org.opentripplanner.transit.model.organization.Agency;

public class AgencyMapper {

  public static List<ApiAgency> mapToApi(Collection<Agency> domain) {
    if (domain == null) {
      return null;
    }
    return domain.stream().map(AgencyMapper::mapToApi).collect(Collectors.toList());
  }

  public static ApiAgency mapToApi(Agency domain) {
    if (domain == null) {
      return null;
    }
    ApiAgency api = new ApiAgency();

    api.id = FeedScopedIdMapper.mapToApi(domain.getId());
    api.name = domain.getName();
    api.url = domain.getUrl();
    api.timezone = domain.getTimezone().getId();
    api.lang = domain.getLang();
    api.phone = domain.getPhone();
    api.fareUrl = domain.getFareUrl();
    api.brandingUrl = domain.getBrandingUrl();

    return api;
  }
}
