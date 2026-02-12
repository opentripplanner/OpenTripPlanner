package org.opentripplanner.framework.snapshot.event;

import org.opentripplanner.framework.snapshot.domain.DomainEvent;
import org.opentripplanner.framework.snapshot.domain.transfer.TransferRepo;

public non-sealed interface DomainEventHandlerTransfers<T extends DomainEvent> extends DomainEventHandler<T> {

    void handle(T event, TransferRepo transfers);
}
