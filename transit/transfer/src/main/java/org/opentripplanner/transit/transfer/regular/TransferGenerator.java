package org.opentripplanner.transit.transfer.regular;

///
/// Generates a profile's regular transfer path templates for all stops. Typically done at graph
/// build time and serialized to reduce startup time.
///
/// @see TransferRealtimeUpdater for the incremental, runtime-mutation counterpart.
///
public interface TransferGenerator {
  void generateTransfersForAllStops();
}
