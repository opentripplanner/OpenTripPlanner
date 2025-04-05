package org.opentripplanner.model.plan.paging.cursor;

import javax.annotation.Nullable;
import org.opentripplanner.framework.model.Cost;
import org.opentripplanner.framework.token.TokenSchema;
import org.opentripplanner.model.plan.ItinerarySortKey;
import org.opentripplanner.model.plan.SortOrder;
import org.opentripplanner.utils.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class PageCursorSerializer {

  private static final byte VERSION_ONE = 1;
  private static final byte VERSION_TWO = 2;
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
  private static final String GENERALIZED_COST_MAX_LIMIT = "generalizedCostMaxLimit";

  private static final TokenSchema SCHEMA_TOKEN = TokenSchema.ofVersion(VERSION_ONE)
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
    // VERSION_TWO
    .newVersion()
    .addInt(GENERALIZED_COST_MAX_LIMIT)
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

    if (cursor.containsItineraryPageCut()) {
      var cut = cursor.itineraryPageCut();
      tokenBuilder
        .withBoolean(CUT_ON_STREET_FIELD, cut.isStreetOnly())
        .withTimeInstant(CUT_DEPARTURE_TIME_FIELD, cut.startTimeAsInstant())
        .withTimeInstant(CUT_ARRIVAL_TIME_FIELD, cut.endTimeAsInstant())
        .withInt(CUT_N_TRANSFERS_FIELD, cut.numberOfTransfers())
        .withInt(CUT_COST_FIELD, cut.generalizedCostIncludingPenalty().toSeconds());
    }
    if (cursor.containsGeneralizedCostMaxLimit()) {
      tokenBuilder.withInt(
        GENERALIZED_COST_MAX_LIMIT,
        cursor.generalizedCostMaxLimit().toSeconds()
      );
    }
    return tokenBuilder.build();
  }

  @Nullable
  public static PageCursor decode(String cursor) {
    if (StringUtils.hasNoValue(cursor)) {
      return null;
    }
    try {
      ItinerarySortKey itineraryPageCut = null;
      var token = SCHEMA_TOKEN.decode(cursor);

      // This throws an exception if an enum is serialized which is not in the code.
      // This is a forward compatibility issue. To avoid this, add the value enum, role out.
      // Start using the enum, roll out again.
      PageType type = token.getEnum(TYPE_FIELD, PageType.class).orElseThrow();
      var edt = token.getTimeInstant(EDT_FIELD).orElse(null);
      var lat = token.getTimeInstant(LAT_FIELD).orElse(null);
      var searchWindow = token.getDuration(SEARCH_WINDOW_FIELD).orElseThrow();
      var originalSortOrder = token.getEnum(SORT_ORDER_FIELD, SortOrder.class).orElseThrow();

      // We use the departure time to determine if the cut is present or not
      var cutDepartureTime = token.getTimeInstant(CUT_DEPARTURE_TIME_FIELD);

      if (cutDepartureTime.isPresent()) {
        itineraryPageCut = new DeduplicationPageCut(
          cutDepartureTime.get(),
          token.getTimeInstant(CUT_ARRIVAL_TIME_FIELD).orElseThrow(),
          Cost.costOfSeconds(token.getInt(CUT_COST_FIELD).orElseThrow()),
          token.getInt(CUT_N_TRANSFERS_FIELD).orElseThrow(),
          token.getBoolean(CUT_ON_STREET_FIELD).orElseThrow()
        );
      }

      // VERSION TWO
      Cost generalizedCostMaxLimit = null;
      if (token.version() >= VERSION_TWO) {
        var cost = token.getInt(GENERALIZED_COST_MAX_LIMIT);
        if (cost.isPresent()) {
          generalizedCostMaxLimit = Cost.costOfSeconds(cost.getAsInt());
        }
      }

      return new PageCursor(
        type,
        originalSortOrder,
        edt,
        lat,
        searchWindow,
        itineraryPageCut,
        generalizedCostMaxLimit
      );
    } catch (Exception e) {
      String details = e.getMessage();
      String message = "Unable to decode page cursor: '" + cursor + "'.";
      if (StringUtils.hasValue(details)) {
        message += " Details: " + details;
      }
      throw new IllegalArgumentException(message, e);
    }
  }
}
