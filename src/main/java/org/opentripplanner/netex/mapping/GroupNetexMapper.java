package org.opentripplanner.netex.mapping;

import com.google.common.collect.ArrayListMultimap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.graph_builder.DataImportIssueStore;
import org.opentripplanner.model.impl.OtpTransitServiceBuilder;
import org.opentripplanner.model.transfer.Transfer;
import org.opentripplanner.netex.mapping.support.FeedScopedIdFactory;
import org.rutebanken.netex.model.ServiceJourneyInterchange;

/**
 * Entities x-referencing entries in more than on independent file must be mapped AFTER all
 * individual files are processed. This class is responsible for the "post-process-mapping",
 * and delegate to mappers for each specific type.
 */
class GroupNetexMapper {
    private final FeedScopedIdFactory idFactory;
    private final DataImportIssueStore issueStore;
    private final OtpTransitServiceBuilder transitBuilder;
    private final List<ServiceJourneyInterchange> interchanges = new ArrayList<>();

    /**
     * A map from trip/serviceJourney id to a ordered list of scheduled stop point ids.
     */
    final ArrayListMultimap<String, String> scheduledStopPointsIndex = ArrayListMultimap.create();

    GroupNetexMapper(
            FeedScopedIdFactory idFactory,
            DataImportIssueStore issueStore,
            OtpTransitServiceBuilder transitBuilder
    ) {
        this.idFactory = idFactory;
        this.issueStore = issueStore;
        this.transitBuilder = transitBuilder;
    }

    void addInterchange(Collection<ServiceJourneyInterchange> interchanges) {
        this.interchanges.addAll(interchanges);
    }

    void mapGroupEntries() {
        mapInterchanges();
    }

    private void mapInterchanges() {
        var mapper = new TransferMapper(
                idFactory,
                issueStore,
                scheduledStopPointsIndex,
                transitBuilder.getTripsById()
        );
        for (ServiceJourneyInterchange it : interchanges) {
            Transfer result = mapper.mapToTransfer(it);
            if(result != null) {
                transitBuilder.getTransfers().add(result);
            }
        }
    }
}
