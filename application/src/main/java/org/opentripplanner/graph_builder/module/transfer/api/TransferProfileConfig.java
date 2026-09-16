package org.opentripplanner.graph_builder.module.transfer.api;

import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.transfer.regular.RaptorTransferProfile;

/**
 * A configured {@code transfers:} profile (see {@code TransferProfilesConfig}), resolved into the
 * types the new raptor-data transfer pipeline needs.
 *
 * @param profileId            this profile's identifying key
 * @param deduplicationProfile another configured profile's key to deduplicate this profile's own
 *                    discovered paths against (see {@code TransferProfilesConfig#deduplicateDelta}),
 *                    {@code null} if this profile has none
 * @param preferences this profile's own preferences, as a full {@code RouteRequest} - the shape
 *                    {@code TransferPathProvider<NearbyStop, RouteRequest>} needs to run a street
 *                    search and cost a path. Only street-search-relevant preference fields are
 *                    populated from config; unrelated {@code RouteRequest} fields (dates,
 *                    itinerary filters, transit preferences, board cost, ...) are left at their
 *                    code defaults and are meaningless here.
 * @param maxDurations at least one entry - the profile's own {@code maxDurations} list, or a
 *                    single unrestricted entry using {@code defaultMaxDuration} if none were
 *                    configured for this profile
 */
public record TransferProfileConfig(
  RaptorTransferProfile profileId,
  @Nullable RaptorTransferProfile deduplicationProfile,
  RouteRequest preferences,
  List<MaxDurationRule> maxDurations
) {}
