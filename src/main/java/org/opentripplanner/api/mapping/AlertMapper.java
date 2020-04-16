package org.opentripplanner.api.mapping;

import org.opentripplanner.api.model.alertpatch.ApiAlert;
import org.opentripplanner.routing.alertpatch.Alert;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class AlertMapper {
    private final Locale locale;

    public AlertMapper(Locale locale) {
        this.locale = locale;
    }

    public List<ApiAlert> mapToApi(Set<Alert> newAlerts) {
        // Using {@code null} and not an empty set will minimize the JSON removing the
        // {@code alerts} from the result.
        if (newAlerts == null || newAlerts.isEmpty()) {
            return null;
        }

        return newAlerts.stream().map(this::mapToApi).collect(Collectors.toList());
    }

    ApiAlert mapToApi(Alert domain) {
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

        api.effectiveStartDate = domain.effectiveStartDate;

        return api;
    }
}
