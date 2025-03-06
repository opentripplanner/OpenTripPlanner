package org.opentripplanner.transit.model.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.transit.model._data.TimetableRepositoryForTest;
import org.opentripplanner.transit.model.basic.SubMode;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Branding;
import org.opentripplanner.transit.model.organization.Operator;

class RouteTest {

  private static final String ID = "1";
  private static final String SHORT_NAME = "short name";
  private static final NonLocalizedString LONG_NAME = new NonLocalizedString("long name");
  private static final String DESCRIPTION = "description";

  private static final BikeAccess BIKE_ACCESS = BikeAccess.ALLOWED;
  private static final TransitMode TRANSIT_MODE = TransitMode.BUS;
  private static final String NETEX_SUBMODE_NAME = "submode";
  private static final SubMode NETEX_SUBMODE = SubMode.of(NETEX_SUBMODE_NAME);
  private static final Operator OPERATOR = Operator.of(FeedScopedId.parse("x:operatorId"))
    .withName("operator name")
    .build();

  private static final Branding BRANDING = Branding.of(FeedScopedId.parse("x:brandingId")).build();
  private static final String COLOR = "color";
  private static final String TEXT_COLOR = "text color";
  private static final int GTFS_TYPE = 0;
  private static final String FLEXIBLE_LINE_TYPE = "flexible line type";
  private static final Integer GTFS_SORT_ORDER = 0;
  private static final String URL = "url";
  public static final Agency AGENCY = TimetableRepositoryForTest.AGENCY;
  private static final Route subject = Route.of(TimetableRepositoryForTest.id(ID))
    .withShortName(SHORT_NAME)
    .withLongName(LONG_NAME)
    .withDescription(DESCRIPTION)
    .withBikesAllowed(BIKE_ACCESS)
    .withMode(TRANSIT_MODE)
    .withNetexSubmode(NETEX_SUBMODE_NAME)
    .withOperator(OPERATOR)
    .withAgency(AGENCY)
    .withBranding(BRANDING)
    .withColor(COLOR)
    .withTextColor(TEXT_COLOR)
    .withGtfsType(GTFS_TYPE)
    .withGtfsSortOrder(GTFS_SORT_ORDER)
    .withFlexibleLineType(FLEXIBLE_LINE_TYPE)
    .withUrl(URL)
    .build();

  @Test
  void copy() {
    assertEquals(ID, subject.getId().getId());

    // Make a copy, and set the same name (nothing is changed)
    var copy = subject.copy().withShortName(SHORT_NAME).build();

    assertSame(subject, copy);

    // Copy and change name
    copy = subject.copy().withShortName("v2").build();

    // The two objects are not the same instance, but are equal(same id)
    assertNotSame(subject, copy);
    assertEquals(subject, copy);

    assertEquals(ID, copy.getId().getId());
    assertEquals("v2", copy.getShortName());
    assertEquals(LONG_NAME, copy.getLongName());
    assertEquals(DESCRIPTION, copy.getDescription());
    assertEquals(BIKE_ACCESS, copy.getBikesAllowed());
    assertEquals(TRANSIT_MODE, copy.getMode());
    assertEquals(NETEX_SUBMODE, copy.getNetexSubmode());
    assertEquals(OPERATOR, copy.getOperator());
    assertEquals(AGENCY, copy.getAgency());
    assertEquals(BRANDING, copy.getBranding());
    assertEquals(COLOR, copy.getColor());
    assertEquals(TEXT_COLOR, copy.getTextColor());
    assertEquals(GTFS_TYPE, copy.getGtfsType());
    assertEquals(GTFS_SORT_ORDER, copy.getGtfsSortOrder());
    assertEquals(FLEXIBLE_LINE_TYPE, copy.getFlexibleLineType());
    assertEquals(URL, copy.getUrl());
  }

  @Test
  void sameAs() {
    assertTrue(subject.sameAs(subject.copy().build()));
    assertFalse(subject.sameAs(subject.copy().withId(TimetableRepositoryForTest.id("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withShortName("X").build()));
    assertFalse(subject.sameAs(subject.copy().withLongName(new NonLocalizedString("X")).build()));
    assertFalse(subject.sameAs(subject.copy().withDescription("X").build()));
    assertFalse(subject.sameAs(subject.copy().withBikesAllowed(BikeAccess.NOT_ALLOWED).build()));
    assertFalse(subject.sameAs(subject.copy().withMode(TransitMode.RAIL).build()));
    assertFalse(subject.sameAs(subject.copy().withNetexSubmode("X").build()));
    assertFalse(
      subject.sameAs(
        subject
          .copy()
          .withOperator(
            Operator.of(FeedScopedId.parse("x:otherOperatorId"))
              .withName("other operator name")
              .build()
          )
          .build()
      )
    );
    assertFalse(
      subject.sameAs(subject.copy().withAgency(TimetableRepositoryForTest.agency("X")).build())
    );
    assertFalse(
      subject.sameAs(
        subject
          .copy()
          .withBranding(Branding.of(FeedScopedId.parse("x:otherBrandingId")).build())
          .build()
      )
    );
    assertFalse(subject.sameAs(subject.copy().withColor("X").build()));
    assertFalse(subject.sameAs(subject.copy().withTextColor("X").build()));
    assertFalse(subject.sameAs(subject.copy().withGtfsType(-1).build()));
    assertFalse(subject.sameAs(subject.copy().withGtfsSortOrder(99).build()));
    assertFalse(subject.sameAs(subject.copy().withFlexibleLineType("X").build()));
    assertFalse(subject.sameAs(subject.copy().withUrl("X").build()));
  }
}
