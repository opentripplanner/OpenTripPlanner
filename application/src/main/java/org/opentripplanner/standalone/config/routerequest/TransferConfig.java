package org.opentripplanner.standalone.config.routerequest;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_0;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.V2_1;

import org.opentripplanner.routing.api.request.preference.TransferOptimizationPreferences;
import org.opentripplanner.routing.api.request.preference.TransferPreferences;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

class TransferConfig {

  static void mapTransferPreferences(NodeAdapter c, TransferPreferences.Builder tx) {
    var dft = tx.original();
    tx
      .withNonpreferredCost(
        c
          .of("nonpreferredTransferPenalty")
          .since(V2_0)
          .summary("Penalty (in seconds) for using a non-preferred transfer.")
          .asInt(dft.nonpreferredCost())
      )
      .withCost(
        c
          .of("transferPenalty")
          .since(V2_0)
          .summary("An additional penalty added to boardings after the first.")
          .description(
            """
            The value is in OTP's internal weight units, which are roughly equivalent to seconds.
            Set this to a high value to discourage transfers. Of course, transfers that save
            significant time or walking will still be taken.
            """
          )
          .asInt(dft.cost())
      )
      .withSlack(
        c
          .of("transferSlack")
          .since(V2_0)
          .summary("The extra time needed to make a safe transfer.")
          .description(
            """
            The extra buffer time/safety margin added to transfers to make sure the connection is safe, time
            wise. We recommend allowing the end-user to set this, and use `board-/alight-slack` to enforce
            agency policies. This time is in addition to how long it might take to walk, board and alight.

            It is useful for passengers on long distance travel, and people with mobility issues, but can be set
            close to zero for everyday commuters and short distance searches in high-frequency transit areas.
            """
          )
          .asDurationOrSeconds(dft.slack())
      )
      .withWaitReluctance(
        c
          .of("waitReluctance")
          .since(V2_0)
          .summary(
            "How much worse is waiting for a transit vehicle than being on a transit vehicle, as a multiplier."
          )
          .asDouble(dft.waitReluctance())
      )
      .withOptimization(
        mapTransferOptimization(
          c
            .of("transferOptimization")
            .since(V2_1)
            .summary("Optimize where a transfer between to trip happens. ")
            .description(
              """
              The main purpose of transfer optimization is to handle cases where it is possible to transfer
              between two routes at more than one point (pair of stops). The transfer optimization ensures that
              transfers occur at the best possible location. By post-processing all paths returned by the router,
              OTP can apply sophisticated calculations that are too slow or not algorithmically valid within
              Raptor. Transfers are optimized is done after the Raptor search and before the paths are passed
              to the itinerary-filter-chain.

              To toggle transfer optimization on or off use the OTPFeature `OptimizeTransfers` (default is on).
              You should leave this on unless there is a critical issue with it. The OTPFeature
              `GuaranteedTransfers` will toggle on and off the priority optimization (part of OptimizeTransfers).

              The optimized transfer service will try to, in order:

              1. Use transfer priority. This includes stay-seated and guaranteed transfers.
              2. Use the transfers with the best distribution of the wait-time, and avoid very short transfers.
              3. Avoid back-travel
              4. Boost stop-priority to select preferred and recommended stops.

              If two paths have the same transfer priority level, then we break the tie by looking at waiting
              times. The goal is to maximize the wait-time for each stop, avoiding situations where there is
              little time available to make the transfer. This is balanced with the generalized-cost. The cost
              is adjusted with a new cost for wait-time (optimized-wait-time-cost).

              The defaults should work fine, but if you have results with short wait-times dominating a better
              option or "back-travel", then try to increase the `minSafeWaitTimeFactor`,
              `backTravelWaitTimeFactor` and/or `extraStopBoardAlightCostsFactor`.

              For details on the logic/design see [transfer optimization](https://github.com/opentripplanner/OpenTripPlanner/blob/dev-2.x/src/main/java/org/opentripplanner/routing/algorithm/transferoptimization/package.md)
              package documentation.
              """
            )
            .asObject()
        )
      );
  }

  private static TransferOptimizationPreferences mapTransferOptimization(NodeAdapter c) {
    var dft = TransferOptimizationPreferences.DEFAULT;
    return TransferOptimizationPreferences.of()
      .withOptimizeTransferWaitTime(
        c
          .of("optimizeTransferWaitTime")
          .since(V2_1)
          .summary("This enables the transfer wait time optimization.")
          .description(
            "If not enabled generalizedCost function is used to pick the optimal transfer point."
          )
          .asBoolean(dft.optimizeTransferWaitTime())
      )
      .withMinSafeWaitTimeFactor(
        c
          .of("minSafeWaitTimeFactor")
          .since(V2_1)
          .summary("Used to set a maximum wait-time cost, base on min-safe-transfer-time.")
          .description(
            "This defines the maximum cost for the logarithmic function relative to the " +
            "min-safe-transfer-time (t0) when wait time goes towards zero(0). f(0) = n * t0"
          )
          .asDouble(dft.minSafeWaitTimeFactor())
      )
      .withBackTravelWaitTimeFactor(
        c
          .of("backTravelWaitTimeFactor")
          .since(V2_1)
          .summary("To reduce back-travel we favor waiting, this reduces the cost of waiting.")
          .description(
            "The wait time is used to prevent *back-travel*, the `backTravelWaitTimeFactor` is " +
            "multiplied with the wait-time and subtracted from the optimized-transfer-cost."
          )
          .asDouble(dft.backTravelWaitTimeFactor())
      )
      .withExtraStopBoardAlightCostsFactor(
        c
          .of("extraStopBoardAlightCostsFactor")
          .since(V2_1)
          .summary("Add an extra board- and alight-cost for prioritized stops.")
          .description(
            """
            A stopBoardAlightTransferCosts is added to the generalized-cost during routing. But this cost
            cannot be too high, because that would add extra cost to the transfer, and favor other
            alternative paths. But, when optimizing transfers, we do not have to take other paths
            into consideration and can *boost* the stop-priority-cost to allow transfers to
            take place at a preferred stop. The cost added during routing is already added to the
            generalized-cost used as a base in the optimized transfer calculation. By setting this
            parameter to 0, no extra cost is added, by setting it to `1.0` the stop-cost is
            doubled. Stop priority is only supported by the NeTEx import, not GTFS.
            """
          )
          .asDouble(dft.extraStopBoardAlightCostsFactor())
      )
      .build();
  }
}
