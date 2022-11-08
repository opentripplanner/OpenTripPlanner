package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.time.LocalDateTime;
import java.util.Collection;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalVersionMapById;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.site.FareZone;
import org.rutebanken.netex.model.TariffZoneRef;
import org.rutebanken.netex.model.TariffZone_VersionStructure;

class TariffZoneMapper {

  private final LocalDateTime startOfPeriod;
  private final FeedScopedIdFactory idFactory;
  private final ReadOnlyHierarchicalVersionMapById<TariffZone_VersionStructure> tariffZonesById;
  private final Multimap<FeedScopedId, FareZone> deduplicateCache = ArrayListMultimap.create();

  TariffZoneMapper(
    LocalDateTime startOfPeriod,
    FeedScopedIdFactory idFactory,
    ReadOnlyHierarchicalVersionMapById<TariffZone_VersionStructure> tariffZonesById
  ) {
    this.startOfPeriod = startOfPeriod;
    this.idFactory = idFactory;
    this.tariffZonesById = tariffZonesById;
  }

  /**
   * Map all current TariffZones.
   */
  Collection<FareZone> listAllCurrentFareZones() {
    return tariffZonesById
      .localListCurrentVersionEntities(startOfPeriod)
      .stream()
      .map(this::mapTariffZone)
      .toList();
  }

  /**
   * Map Netex TariffZone to OTP TariffZone
   */
  FareZone findAndMapTariffZone(TariffZoneRef ref) {
    var tariffZone = tariffZonesById.lookup(ref, startOfPeriod);
    return (tariffZone == null) ? null : mapTariffZone(tariffZone);
  }

  /**
   * Map Netex TariffZone to OTP TariffZone
   */
  private FareZone mapTariffZone(TariffZone_VersionStructure tariffZone) {
    if (tariffZone == null) {
      return null;
    }

    FeedScopedId id = idFactory.createId(tariffZone.getId());
    String name = tariffZone.getName().getValue();
    return deduplicate(FareZone.of(id).withName(name).build());
  }

  private FareZone deduplicate(FareZone candidate) {
    var existing = deduplicateCache
      .get(candidate.getId())
      .stream()
      .filter(candidate::sameAs)
      .findFirst();

    if (existing.isPresent()) {
      return existing.get();
    }

    deduplicateCache.put(candidate.getId(), candidate);
    return candidate;
  }
}
