package org.opentripplanner.updater.alerts;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.transit.realtime.GtfsRealtime;
import com.google.transit.realtime.GtfsRealtime.Alert;
import com.google.transit.realtime.GtfsRealtime.Alert.Cause;
import com.google.transit.realtime.GtfsRealtime.Alert.Effect;
import com.google.transit.realtime.GtfsRealtime.Alert.SeverityLevel;
import com.google.transit.realtime.GtfsRealtime.TranslatedString.Translation;
import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.HashSet;
import org.opentripplanner.routing.alertpatch.AlertCause;
import org.opentripplanner.routing.alertpatch.AlertEffect;
import org.opentripplanner.routing.alertpatch.AlertSeverity;
import org.opentripplanner.routing.alertpatch.EntitySelector;
import org.opentripplanner.routing.alertpatch.TransitAlert;
import org.opentripplanner.routing.services.TransitAlertService;
import org.opentripplanner.util.TranslatedString;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(MockitoJUnitRunner.class)
public class AlertsUpdateHandlerTest {

    private AlertsUpdateHandler handler;

    @Spy
    private FakeTransitAlertService service;

    @Before
    public void setUp() {
        handler = new AlertsUpdateHandler();
        handler.setFeedId("1");
        handler.setEarlyStart(5);
        handler.setTransitAlertService(service);
    }

    @Test
    public void testAlertWithTimePeriodConsideringEarlyStart() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addActivePeriod(
                        GtfsRealtime.TimeRange.newBuilder().setStart(10).setEnd(20).build())
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(new Date(5 * 1000), transitAlert.getEffectiveStartDate());
        assertEquals(new Date(20 * 1000), transitAlert.getEffectiveEndDate());
    }

    @Test
    public void testAlertStartConsideringEarlyStart() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addActivePeriod(GtfsRealtime.TimeRange.newBuilder().setStart(10).build())
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(new Date(5 * 1000), transitAlert.getEffectiveStartDate());
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
        assertEquals(new Date(20 * 1000), transitAlert.getEffectiveEndDate());
    }

    @Test
    public void testWithoutUrl() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertNull(transitAlert.alertUrl);
    }

    @Test
    public void testWithoutUrlTranslations() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setUrl(GtfsRealtime.TranslatedString.newBuilder()
                        .addTranslation(0, Translation.newBuilder()
                                .setText("https://www.opentripplanner.org/")
                                .build())
                        .build())
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals("https://www.opentripplanner.org/", transitAlert.alertUrl.toString());
    }

    @Test
    public void testWithUrlTranslations() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setUrl(GtfsRealtime.TranslatedString.newBuilder()
                        .addTranslation(0, Translation.newBuilder()
                                .setText("https://www.opentripplanner.org/")
                                .setLanguage("en")
                                .build())
                        .addTranslation(0, Translation.newBuilder()
                                .setText("https://www.opentripplanner.org/fr")
                                .setLanguage("fr")
                                .build())
                        .build())
                .build();
        TransitAlert transitAlert = processOneAlert(alert);

        List<Entry<String, String>> translations =
                ((TranslatedString) transitAlert.alertUrl).getTranslations();
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
                .setHeaderText(GtfsRealtime.TranslatedString.newBuilder()
                        .addTranslation(0, Translation.newBuilder().setText("Title").build())
                        .build())
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals("Title", transitAlert.alertHeaderText.toString());
    }

    @Test
    public void testWithHeaderTranslations() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setHeaderText(GtfsRealtime.TranslatedString.newBuilder()
                        .addTranslation(
                                0,
                                Translation.newBuilder().setText("Title").setLanguage("en").build()
                        )
                        .addTranslation(0,
                                Translation.newBuilder().setText("Titre").setLanguage("fr").build()
                        )
                        .build())
                .build();
        TransitAlert transitAlert = processOneAlert(alert);

        List<Entry<String, String>> translations =
                ((TranslatedString) transitAlert.alertHeaderText).getTranslations();
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
                .setDescriptionText(GtfsRealtime.TranslatedString.newBuilder()
                        .addTranslation(0, Translation.newBuilder().setText("Description").build())
                        .build())
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals("Description", transitAlert.alertDescriptionText.toString());
    }

    @Test
    public void testWithDescriptionTranslations() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setDescriptionText(GtfsRealtime.TranslatedString.newBuilder()
                        .addTranslation(
                                0, Translation.newBuilder()
                                        .setText("Description")
                                        .setLanguage("en")
                                        .build())
                        .addTranslation(0, Translation.newBuilder()
                                .setText("La description")
                                .setLanguage("fr")
                                .build())
                        .build())
                .build();
        TransitAlert transitAlert = processOneAlert(alert);

        List<Entry<String, String>> translations =
                ((TranslatedString) transitAlert.alertDescriptionText).getTranslations();
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
        assertEquals(AlertSeverity.UNKNOWN_SEVERITY, transitAlert.severity);
    }

    @Test
    public void testSetAlertSeverity() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setSeverityLevel(SeverityLevel.SEVERE)
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(AlertSeverity.SEVERE, transitAlert.severity);
    }

    @Test
    public void testMissingAlertCause() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(AlertCause.UNKNOWN_CAUSE, transitAlert.cause);
    }

    @Test
    public void testSetAlertCause() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setCause(Cause.MAINTENANCE)
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(AlertCause.MAINTENANCE, transitAlert.cause);
    }

    @Test
    public void testMissingAlertEffect() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(AlertEffect.UNKNOWN_EFFECT, transitAlert.effect);
    }

    @Test
    public void testSetAlertEffect() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .setEffect(Effect.MODIFIED_SERVICE)
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        assertEquals(AlertEffect.MODIFIED_SERVICE, transitAlert.effect);
    }

    @Test
    public void testAgencySelector() {
        GtfsRealtime.Alert alert = GtfsRealtime.Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setAgencyId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(1l, totalSelectorCount);
        long agencySelectorCount = transitAlert.getEntities()
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
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(1l, totalSelectorCount);
        long routeSelectorCount = transitAlert.getEntities()
                .stream()
                .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
                .count();
        assertEquals(1l, routeSelectorCount);
    }

    @Test
    public void testTripSelectorWithTripId() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(GtfsRealtime.EntitySelector.newBuilder().setTrip(
                        TripDescriptor.newBuilder().setTripId("1").build()))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(1l, totalSelectorCount);
        long tripSelectorCount = transitAlert.getEntities()
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
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(1l, totalSelectorCount);
        long stopSelectorCount = transitAlert.getEntities()
                .stream()
                .filter(entitySelector -> entitySelector instanceof EntitySelector.Stop)
                .count();
        assertEquals(1l, stopSelectorCount);
    }

    @Test
    public void testStopAndRouteSelector() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(
                        0, GtfsRealtime.EntitySelector.newBuilder().setStopId("1").setRouteId("1"))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(1l, totalSelectorCount);
        long stopAndRouteSelectorCount = transitAlert.getEntities()
                .stream()
                .filter(entitySelector -> entitySelector instanceof EntitySelector.StopAndRoute)
                .count();
        assertEquals(1l, stopAndRouteSelectorCount);
    }

    @Test
    public void testStopAndTripSelector() {
        GtfsRealtime.Alert alert = Alert.newBuilder()
                .addInformedEntity(
                        0, GtfsRealtime.EntitySelector.newBuilder().setStopId("1").setTrip(
                                TripDescriptor.newBuilder().setTripId("1").build()))
                .build();
        TransitAlert transitAlert = processOneAlert(alert);
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(1l, totalSelectorCount);
        long stopAndTripSelectorCount = transitAlert.getEntities()
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
        long totalSelectorCount = transitAlert.getEntities().size();
        assertEquals(3l, totalSelectorCount);
        long agencySelectorCount = transitAlert.getEntities()
                .stream()
                .filter(entitySelector -> entitySelector instanceof EntitySelector.Agency)
                .count();
        assertEquals(2l, agencySelectorCount);
        long routeSelectorCount = transitAlert.getEntities()
                .stream()
                .filter(entitySelector -> entitySelector instanceof EntitySelector.Route)
                .count();
        assertEquals(1l, routeSelectorCount);
    }

    private TransitAlert processOneAlert(GtfsRealtime.Alert alert) {
        GtfsRealtime.FeedMessage message = GtfsRealtime.FeedMessage.newBuilder()
                .setHeader(GtfsRealtime.FeedHeader.newBuilder().setGtfsRealtimeVersion("2.0"))
                .addEntity(GtfsRealtime.FeedEntity.newBuilder().setAlert(alert).setId("1")).build();
        handler.update(message);
        Collection<TransitAlert> alerts = service.getAllAlerts();
        assertEquals(1, alerts.size());
        return alerts.iterator().next();
    }

    static abstract class FakeTransitAlertService implements TransitAlertService {

        private Multimap<EntitySelector, TransitAlert> alerts = HashMultimap.create();

        @Override
        public Collection<TransitAlert> getAllAlerts() {
            return new HashSet<>(alerts.values());
        }

        @Override
        public void setAlerts(Collection<TransitAlert> alerts) {
            Multimap<EntitySelector, TransitAlert> newAlerts = HashMultimap.create();
            for (TransitAlert alert : alerts) {
                for (EntitySelector entity : alert.getEntities()) {
                    newAlerts.put(entity, alert);
                }
            }

            this.alerts = newAlerts;
        }
    }
}