package io.librevents.chain.block.message;

import io.librevents.dto.message.MessageDetails;
import io.librevents.integration.broadcast.blockchain.BlockchainEventBroadcaster;
import io.librevents.integration.eventstore.EventStore;
import io.librevents.integration.eventstore.SaveableEventStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BroadcastingMessageListener implements MessageListener {

    private final EventStore eventStore;
    private final BlockchainEventBroadcaster eventBroadcaster;

    @Autowired
    public BroadcastingMessageListener(
            EventStore eventStore, BlockchainEventBroadcaster eventBroadcaster) {
        this.eventStore = eventStore;
        this.eventBroadcaster = eventBroadcaster;
    }

    @Override
    public void onMessage(MessageDetails messageDetails) {
        eventBroadcaster.broadcastMessage(messageDetails);
        if (eventStore instanceof SaveableEventStore saveableEventStore) {
            saveableEventStore.save(messageDetails);
        }
    }
}
