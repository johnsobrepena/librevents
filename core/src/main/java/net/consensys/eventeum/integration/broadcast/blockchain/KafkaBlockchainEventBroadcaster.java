/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.consensys.eventeum.integration.broadcast.blockchain;

import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import net.consensys.eventeum.dto.block.BlockDetails;
import net.consensys.eventeum.dto.event.ContractEventDetails;
import net.consensys.eventeum.dto.event.filter.ContractEventFilter;
import net.consensys.eventeum.dto.message.*;
import net.consensys.eventeum.dto.transaction.TransactionDetails;
import net.consensys.eventeum.integration.KafkaSettings;
import net.consensys.eventeum.utils.JSON;
import org.springframework.data.repository.CrudRepository;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * A BlockchainEventBroadcaster that broadcasts the events to a Kafka queue.
 *
 * <p>The key for each message will be defined by the correlationIdStrategy if configured, or a
 * combination of the transactionHash, blockHash and logIndex otherwise.
 *
 * <p>The topic names for block and contract events can be configured via the
 * kafka.topic.contractEvents and kafka.topic.blockEvents properties.
 *
 * @author Craig Williams craig.williams@consensys.net
 */
@Slf4j
public class KafkaBlockchainEventBroadcaster implements BlockchainEventBroadcaster {

    private final KafkaTemplate<String, EventeumMessage> kafkaTemplate;
    private final KafkaSettings kafkaSettings;
    private final CrudRepository<ContractEventFilter, String> filterRepository;

    public KafkaBlockchainEventBroadcaster(
            KafkaTemplate<String, EventeumMessage> kafkaTemplate,
            KafkaSettings kafkaSettings,
            CrudRepository<ContractEventFilter, String> filterRepository) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaSettings = kafkaSettings;
        this.filterRepository = filterRepository;
    }

    @Override
    public void broadcastNewBlock(BlockDetails block) {
        final EventeumMessage<BlockDetails> message = createBlockEventMessage(block);
        log.info("Sending block message: ${}", JSON.stringify(message));

        kafkaTemplate.send(kafkaSettings.getBlockEventsTopic(), message.getId(), message);
    }

    @Override
    public void broadcastContractEvent(ContractEventDetails eventDetails) {
        final EventeumMessage<ContractEventDetails> message =
                createContractEventMessage(eventDetails);
        log.info("Sending contract event message: {}", JSON.stringify(message));

        kafkaTemplate.send(
                kafkaSettings.getContractEventsTopic(),
                getContractEventCorrelationId(message),
                message);
    }

    @Override
    public void broadcastTransaction(TransactionDetails transactionDetails) {
        final EventeumMessage<TransactionDetails> message =
                createTransactionEventMessage(transactionDetails);
        log.info("Sending transaction event message: {}", JSON.stringify(message));

        kafkaTemplate.send(
                kafkaSettings.getTransactionEventsTopic(),
                transactionDetails.getBlockHash(),
                message);
    }

    @Override
    public void broadcastMessage(MessageDetails messageDetails) {
        final EventeumMessage<MessageDetails> message = createMessageEventMessage(messageDetails);
        log.info("Sending event message: {}", JSON.stringify(message));

        kafkaTemplate.send(
                kafkaSettings.getMessageEventsTopic(), messageDetails.getTopicId(), message);
    }

    protected EventeumMessage<BlockDetails> createBlockEventMessage(BlockDetails blockDetails) {
        return new BlockEvent(blockDetails);
    }

    protected EventeumMessage<ContractEventDetails> createContractEventMessage(
            ContractEventDetails contractEventDetails) {
        return new ContractEvent(contractEventDetails);
    }

    protected EventeumMessage<TransactionDetails> createTransactionEventMessage(
            TransactionDetails transactionDetails) {
        return new TransactionEvent(transactionDetails);
    }

    protected EventeumMessage<MessageDetails> createMessageEventMessage(
            MessageDetails messageDetails) {
        return new MessageEvent(messageDetails);
    }

    private String getContractEventCorrelationId(EventeumMessage<ContractEventDetails> message) {
        final Optional<ContractEventFilter> filter =
                filterRepository.findById(message.getDetails().getFilterId());

        if (!filter.isPresent() || filter.get().getCorrelationIdStrategy() == null) {
            return message.getId();
        }

        return filter.get().getCorrelationIdStrategy().getCorrelationId(message.getDetails());
    }
}
