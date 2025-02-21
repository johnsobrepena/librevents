package io.librevents.chain.contract;

import io.librevents.dto.event.ContractEventDetails;
import io.librevents.integration.eventstore.EventStore;

public abstract class BaseContractEventListener implements ContractEventListener {

    protected EventStore eventStore;

    BaseContractEventListener(EventStore eventStore) {
        this.eventStore = eventStore;
    }

    protected boolean isExistingEvent(ContractEventDetails eventDetails) {
        return eventStore
                .getContractEvent(
                        eventDetails.getEventSpecificationSignature(),
                        eventDetails.getAddress(),
                        eventDetails.getBlockHash(),
                        eventDetails.getTransactionHash(),
                        eventDetails.getLogIndex())
                .isEmpty();
    }
}
