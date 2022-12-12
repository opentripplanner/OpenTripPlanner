package org.opentripplanner.ext.legacygraphqlapi.datafetchers;

import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.opentripplanner.api.mapping.StreetNoteMaperMapper;
import org.opentripplanner.ext.legacygraphqlapi.generated.LegacyGraphQLDataFetchers;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.plan.ElevationProfile.Step;
import org.opentripplanner.model.plan.WalkStep;
import org.opentripplanner.routing.alertpatch.TimePeriod;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.street.model.note.StreetNote;

public class LegacyGraphQLstepImpl implements LegacyGraphQLDataFetchers.LegacyGraphQLStep {

  @Override
  public DataFetcher<String> absoluteDirection() {
    return environment -> getSource(environment).getAbsoluteDirection().toString();
  }

  @Override
  public DataFetcher<Iterable<TransitAlert>> alerts() {
    return environment ->
      getSource(environment)
        .getStreetNotes()
        .stream()
        .map(LegacyGraphQLstepImpl::mapStreetNoteToAlert)
        .toList();
  }

  @Override
  public DataFetcher<Boolean> area() {
    return environment -> getSource(environment).getArea();
  }

  @Override
  public DataFetcher<Boolean> bogusName() {
    return environment -> getSource(environment).getBogusName();
  }

  @Override
  public DataFetcher<Double> distance() {
    return environment -> getSource(environment).getDistance();
  }

  @Override
  public DataFetcher<Iterable<Step>> elevationProfile() {
    return environment -> getSource(environment).getElevationProfile().steps();
  }

  @Override
  public DataFetcher<String> exit() {
    return environment -> getSource(environment).getExit();
  }

  @Override
  public DataFetcher<Double> lat() {
    return environment -> getSource(environment).getStartLocation().latitude();
  }

  @Override
  public DataFetcher<Double> lon() {
    return environment -> getSource(environment).getStartLocation().longitude();
  }

  @Override
  public DataFetcher<String> relativeDirection() {
    return environment -> getSource(environment).getRelativeDirection().toString();
  }

  @Override
  public DataFetcher<Boolean> stayOn() {
    return environment -> getSource(environment).getStayOn();
  }

  @Override
  public DataFetcher<String> streetName() {
    return environment -> getSource(environment).getStreetName().toString(environment.getLocale());
  }

  @Override
  public DataFetcher<Boolean> walkingBike() {
    return environment -> getSource(environment).isWalkingBike();
  }

  private WalkStep getSource(DataFetchingEnvironment environment) {
    return environment.getSource();
  }

  /**
   * Similar to {@link StreetNoteMaperMapper::mapToApi}.
   */
  public static TransitAlert mapStreetNoteToAlert(StreetNote note) {
    TransitAlert alert = new TransitAlert();
    alert.alertHeaderText = note.note;
    alert.alertDescriptionText = note.descriptionText;
    alert.alertUrl = new NonLocalizedString(note.url);
    alert.setTimePeriods(
      List.of(
        new TimePeriod(
          note.effectiveStartDate.getTime() / 1000,
          note.effectiveEndDate.getTime() / 1000
        )
      )
    );
    return alert;
  }
}
