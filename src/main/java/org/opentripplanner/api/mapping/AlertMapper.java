package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.ApiAlert;
import org.opentripplanner.routing.alertpatch.TransitAlert;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class AlertMapper {

  private final Locale locale;

  public AlertMapper(Locale locale) {
    this.locale = locale;
  }

  public List<ApiAlert> mapToApi(Collection<TransitAlert> alerts) {
    // Using {@code null} and not an empty set will minimize the JSON removing the
    // {@code alerts} from the result.
    if (alerts == null || alerts.isEmpty()) {
      return null;
    }

    return alerts.stream().map(this::mapToApi).collect(Collectors.toList());
  }

  ApiAlert mapToApi(TransitAlert domain) {
    ApiAlert api = new ApiAlert();
    if (domain.alertHeaderText != null) {
      api.alertHeaderText = domain.alertHeaderText.toString(locale);
    }

    if (domain.alertDescriptionText != null) {
      api.alertDescriptionText = domain.alertDescriptionText.toString(locale);
    }

    if (domain.alertUrl != null) {
      api.alertUrl = domain.alertUrl.toString(locale);
    }

    api.effectiveStartDate = domain.getEffectiveStartDate();
    api.effectiveEndDate = domain.getEffectiveEndDate();

    return api;
  }
}
