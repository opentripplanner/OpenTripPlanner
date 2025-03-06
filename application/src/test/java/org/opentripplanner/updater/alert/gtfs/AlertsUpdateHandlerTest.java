package org.opentripplanner.updater.alert.gtfs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.Alert;
import com.google.transit.realtime.GtfsRealtime.Alert.Cause;
import com.google.transit.realtime.GtfsRealtime.Alert.Effect;
import com.google.transit.realtime.GtfsRealtime.Alert.SeverityLevel;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.TranslatedString;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.impl.TransitAlertServiceImpl;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.transit.service.TimetableRepository;

public class AlertsUpdateHandlerTest {

  private AlertsUpdateHandler handler;

  private final TransitAlertService service = new TransitAlertServiceImpl(
    new TimetableRepository()
  );

  @BeforeEach
  public void setUp() {
    handler = new AlertsUpdateHandler(false);
    handler.setFeedId("1");
    handler.setEarlyStart(5);
    handler.setTransitAlertService(service);
  }

  @Test
  public void testAlertWithTimePeriodConsideringEarlyStart() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addActivePeriod(GtfsRealtime.TimeRange.newBuilder().setStart(10).setEnd(20).build())
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(Instant.ofEpochSecond(5), transitAlert.getEffectiveStartDate());
    assertEquals(Instant.ofEpochSecond(20), transitAlert.getEffectiveEndDate());
  }

  @Test
  public void testAlertStartConsideringEarlyStart() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addActivePeriod(GtfsRealtime.TimeRange.newBuilder().setStart(10).build())
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(Instant.ofEpochSecond(5), transitAlert.getEffectiveStartDate());
    assertNull(transitAlert.getEffectiveEndDate());
  }

  @Test
  public void testAlertEnd() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addActivePeriod(GtfsRealtime.TimeRange.newBuilder().setEnd(20).build())
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertNull(transitAlert.getEffectiveStartDate());
    assertEquals(Instant.ofEpochSecond(20), transitAlert.getEffectiveEndDate());
  }

  @Test
  public void testWithoutUrl() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(Optional.empty(), transitAlert.url());
  }

  @Test
  public void testWithoutUrlTranslations() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setUrl(
        GtfsRealtime.TranslatedString.newBuilder()
          .addTranslation(
            0,
            Translation.newBuilder().setText("https://www.opentripplanner.org/").build()
          )
          .build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals("https://www.opentripplanner.org/", transitAlert.url().get().toString());
  }

  @Test
  public void testWithUrlTranslations() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setUrl(
        GtfsRealtime.TranslatedString.newBuilder()
          .addTranslation(
            0,
            Translation.newBuilder()
              .setText("https://www.opentripplanner.org/")
              .setLanguage("en")
              .build()
          )
          .addTranslation(
            0,
            Translation.newBuilder()
              .setText("https://www.opentripplanner.org/fr")
              .setLanguage("fr")
              .build()
          )
          .build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);

    List<Entry<String, String>> translations =
      ((TranslatedString) transitAlert.url().get()).getTranslations();
    assertEquals(2, translations.size());
    assertEquals("en", translations.get(0).getKey());
    assertEquals("https://www.opentripplanner.org/", translations.get(0).getValue());
    assertEquals("fr", translations.get(1).getKey());
    assertEquals("https://www.opentripplanner.org/fr", translations.get(1).getValue());
  }

  @Test
  public void testWithoutHeaderTranslations() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setHeaderText(
        GtfsRealtime.TranslatedString.newBuilder()
          .addTranslation(0, Translation.newBuilder().setText("Title").build())
          .build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals("Title", transitAlert.headerText().get().toString());
  }

  @Test
  public void testWithHeaderTranslations() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setHeaderText(
        GtfsRealtime.TranslatedString.newBuilder()
          .addTranslation(0, Translation.newBuilder().setText("Title").setLanguage("en").build())
          .addTranslation(0, Translation.newBuilder().setText("Titre").setLanguage("fr").build())
          .build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);

    List<Entry<String, String>> translations =
      ((TranslatedString) transitAlert.headerText().get()).getTranslations();
    assertEquals(2, translations.size());
    assertEquals("en", translations.get(0).getKey());
    assertEquals("Title", translations.get(0).getValue());
    assertEquals("fr", translations.get(1).getKey());
    assertEquals("Titre", translations.get(1).getValue());
  }

  @Test
  public void testWithoutDescriptionTranslations() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setDescriptionText(
        GtfsRealtime.TranslatedString.newBuilder()
          .addTranslation(0, Translation.newBuilder().setText("Description").build())
          .build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals("Description", transitAlert.descriptionText().get().toString());
  }

  @Test
  public void testWithDescriptionTranslations() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setDescriptionText(
        GtfsRealtime.TranslatedString.newBuilder()
          .addTranslation(
            0,
            Translation.newBuilder().setText("Description").setLanguage("en").build()
          )
          .addTranslation(
            0,
            Translation.newBuilder().setText("La description").setLanguage("fr").build()
          )
          .build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);

    List<Entry<String, String>> translations =
      ((TranslatedString) transitAlert.descriptionText().get()).getTranslations();
    assertEquals(2, translations.size());
    assertEquals("en", translations.get(0).getKey());
    assertEquals("Description", translations.get(0).getValue());
    assertEquals("fr", translations.get(1).getKey());
    assertEquals("La description", translations.get(1).getValue());
  }

  @Test
  public void testMissingAlertSeverity() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(AlertSeverity.UNKNOWN_SEVERITY, transitAlert.severity());
  }

  @Test
  public void testSetAlertSeverity() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setSeverityLevel(SeverityLevel.SEVERE)
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(AlertSeverity.SEVERE, transitAlert.severity());
  }

  @Test
  public void testMissingAlertCause() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(AlertCause.UNKNOWN_CAUSE, transitAlert.cause());
  }

  @Test
  public void testSetAlertCause() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setCause(Cause.MAINTENANCE)
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(AlertCause.MAINTENANCE, transitAlert.cause());
  }

  @Test
  public void testMissingAlertEffect() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(AlertEffect.UNKNOWN_EFFECT, transitAlert.effect());
  }

  @Test
  public void testSetAlertEffect() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .setEffect(Effect.MODIFIED_SERVICE)
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    assertEquals(AlertEffect.MODIFIED_SERVICE, transitAlert.effect());
  }

  @Test
  public void testAgencySelector() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long agencySelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Agency)
      .count();
    assertEquals(1l, agencySelectorCount);
  }

  @Test
  public void testRouteSelector() {
    GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setRouteId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long routeSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
      .count();
    assertEquals(1l, routeSelectorCount);
  }

  @Test
  public void testTripSelectorWithTripId() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(
        GtfsRealtime.EntitySelector.newBuilder()
          .setTrip(TripDescriptor.newBuilder().setTripId("1").build())
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long tripSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Trip)
      .count();
    assertEquals(1l, tripSelectorCount);
  }

  @Test
  public void testStopSelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setStopId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long stopSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
      .count();
    assertEquals(1l, stopSelectorCount);
  }

  @Test
  public void testStopAndRouteSelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(0, GtfsRealtime.EntitySelector.newBuilder().setStopId("1").setRouteId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long stopAndRouteSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.StopAndRoute)
      .count();
    assertEquals(1l, stopAndRouteSelectorCount);
  }

  @Test
  public void testStopAndTripSelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(
        0,
        GtfsRealtime.EntitySelector.newBuilder()
          .setStopId("1")
          .setTrip(TripDescriptor.newBuilder().setTripId("1").build())
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long stopAndTripSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.StopAndTrip)
      .count();
    assertEquals(1l, stopAndTripSelectorCount);
  }

  @Test
  public void testMultipleSelectors() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("2"))
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setRouteId("1"))
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(3l, totalSelectorCount);
    long agencySelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Agency)
      .count();
    assertEquals(2l, agencySelectorCount);
    long routeSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
      .count();
    assertEquals(1l, routeSelectorCount);
  }

  @Test
  public void testMissingSelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder().build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    List<EntitySelector> selectors = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Unknown)
      .toList();
    assertEquals(1l, selectors.size());
    assertEquals(
      "Alert had no entities",
      ((EntitySelector.Unknown) selectors.get(0)).description()
    );
  }

  @Test
  public void testUnknownSelector() {
    // Setting just direction is not supported and should result in entity not being handled
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setDirectionId(1).build())
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    List<EntitySelector> selectors = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.Unknown)
      .toList();
    assertEquals(1l, selectors.size());
    assertEquals(
      "Entity selector: direction_id: 1\n",
      ((EntitySelector.Unknown) selectors.get(0)).description()
    );
  }

  @Test
  public void testDirectionAndRouteSelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(
        GtfsRealtime.EntitySelector.newBuilder().setDirectionId(1).setRouteId("1").build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long directionAndRouteSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.DirectionAndRoute)
      .count();
    assertEquals(1l, directionAndRouteSelectorCount);
  }

  @Test
  public void testRouteTypeSelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setRouteType(1).build())
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long RouteTypeSelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.RouteType)
      .count();
    assertEquals(1l, RouteTypeSelectorCount);
  }

  @Test
  public void testRouteTypeAndAgencySelector() {
    GtfsRealtime.Alert alert = Alert.newBuilder()
      .addInformedEntity(
        GtfsRealtime.EntitySelector.newBuilder().setRouteType(1).setAgencyId("1").build()
      )
      .build();
    TransitAlert transitAlert = processOneAlert(alert);
    long totalSelectorCount = transitAlert.entities().size();
    assertEquals(1l, totalSelectorCount);
    long RouteTypeAndAgencySelectorCount = transitAlert
      .entities()
      .stream()
      .filter(entitySelector -> entitySelector instanceof EntitySelector.RouteTypeAndAgency)
      .count();
    assertEquals(1l, RouteTypeAndAgencySelectorCount);
  }

  private TransitAlert processOneAlert(GtfsRealtime.Alert alert) {
    GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.newBuilder()
      .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
      .addEntity(GtfsRealtime.FeedEntity.newBuilder().setAlert(alert).setId("1"))
      .build();
    handler.update(message, null);
    Collection<TransitAlert> alerts = service.getAllAlerts();
    assertEquals(1, alerts.size());
    return alerts.iterator().next();
  }
}
