package org.opentripplanner.model.plan.paging.cursor;

import javax.annotation.Nullable;
import org.opentripplanner.framework.token.TokenSchema;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PageCursorSerializer {

  private static final byte VERSION = 1;
  private static final Logger LOG = LoggerFactory.getLogger(PageCursor.class);

  private static final String TYPE_FIELD = "Type";
  private static final String EDT_FIELD = "EDT";
  private static final String LAT_FIELD = "LAT";
  private static final String SEARCH_WINDOW_FIELD = "SW";
  private static final String SORT_ORDER_FIELD = "SortOrder";
  private static final String CUT_ON_STREET_FIELD = "cutOnStreet";
  private static final String CUT_DEPARTURE_TIME_FIELD = "cutDepartureTime";
  private static final String CUT_ARRIVAL_TIME_FIELD = "cutArrivalTime";
  private static final String CUT_N_TRANSFERS_FIELD = "cutTx";
  private static final String CUT_COST_FIELD = "cutCost";

  private static final TokenSchema SCHEMA_TOKEN = TokenSchema.ofVersion(VERSION)
    .addEnum(TYPE_FIELD)
    .addTimeInstant(EDT_FIELD)
    .addTimeInstant(LAT_FIELD)
    .addDuration(SEARCH_WINDOW_FIELD)
    .addEnum(SORT_ORDER_FIELD)
    .addBoolean(CUT_ON_STREET_FIELD)
    .addTimeInstant(CUT_DEPARTURE_TIME_FIELD)
    .addTimeInstant(CUT_ARRIVAL_TIME_FIELD)
    .addInt(CUT_N_TRANSFERS_FIELD)
    .addInt(CUT_COST_FIELD)
    .build();

  /** private constructor to prevent instantiating this utility class */
  private PageCursorSerializer() {}

  @Nullable
  public static String encode(PageCursor cursor) {
    var tokenBuilder = SCHEMA_TOKEN.encode()
      .withEnum(TYPE_FIELD, cursor.type())
      .withTimeInstant(EDT_FIELD, cursor.earliestDepartureTime())
      .withTimeInstant(LAT_FIELD, cursor.latestArrivalTime())
      .withDuration(SEARCH_WINDOW_FIELD, cursor.searchWindow())
      .withEnum(SORT_ORDER_FIELD, cursor.originalSortOrder());

    var cut = cursor.itineraryPageCut();
    if (cut != null) {
      tokenBuilder
        .withBoolean(CUT_ON_STREET_FIELD, cut.isOnStreetAllTheWay())
        .withTimeInstant(CUT_DEPARTURE_TIME_FIELD, cut.startTimeAsInstant())
        .withTimeInstant(CUT_ARRIVAL_TIME_FIELD, cut.endTimeAsInstant())
        .withInt(CUT_N_TRANSFERS_FIELD, cut.getNumberOfTransfers())
        .withInt(CUT_COST_FIELD, cut.getGeneralizedCostIncludingPenalty());
    }

    return tokenBuilder.build();
  }

  @Nullable
  public static PageCursor decode(String cursor) {
    if (StringUtils.hasNoValueOrNullAsString(cursor)) {
      return null;
    }
    try {
      ItinerarySortKey itineraryPageCut = null;
      var token = SCHEMA_TOKEN.decode(cursor);

      // This throws an exception if an enum is serialized which is not in the code.
      // This is a forward compatibility issue. To avoid this, add the value enum, role out.
      // Start using the enum, roll out again.
      PageType type = token.getEnum(TYPE_FIELD, PageType.class).orElseThrow();
      var edt = token.getTimeInstant(EDT_FIELD);
      var lat = token.getTimeInstant(LAT_FIELD);
      var searchWindow = token.getDuration(SEARCH_WINDOW_FIELD);
      var originalSortOrder = token.getEnum(SORT_ORDER_FIELD, SortOrder.class).orElseThrow();

      // We use the departure time to determine if the cut is present or not
      var cutDepartureTime = token.getTimeInstant(CUT_DEPARTURE_TIME_FIELD);

      if (cutDepartureTime != null) {
        itineraryPageCut = new DeduplicationPageCut(
          cutDepartureTime,
          token.getTimeInstant(CUT_ARRIVAL_TIME_FIELD),
          token.getInt(CUT_COST_FIELD),
          token.getInt(CUT_N_TRANSFERS_FIELD),
          token.getBoolean(CUT_ON_STREET_FIELD)
        );
      }

      // Add logic to read in data from next version here.
      // if(token.version() > 1) { /* get v2 here */}

      return new PageCursor(type, originalSortOrder, edt, lat, searchWindow, itineraryPageCut);
    } catch (Exception e) {
      String details = e.getMessage();
      if (StringUtils.hasValue(details)) {
        LOG.warn("Unable to decode page cursor: '{}'. Details: {}", cursor, details);
      } else {
        LOG.warn("Unable to decode page cursor: '{}'.", cursor);
      }
      return null;
    }
  }
}
