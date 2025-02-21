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

package io.librevents.integration.broadcast.internal;

import io.librevents.dto.event.filter.ContractEventFilter;
import io.librevents.dto.message.*;
import io.librevents.integration.KafkaSettings;
import io.librevents.model.TransactionMonitoringSpec;
import io.librevents.utils.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * An EventeumEventBroadcaster that broadcasts the events to a Kafka queue.
 *
 * <p>The topic name can be configured via the kafka.topic.eventeumEvents property.
 *
 * @author Craig Williams craig.williams@consensys.net
 */
@Slf4j
public class KafkaLibreventsEventBroadcaster implements LibreventsEventBroadcaster {

    private final KafkaTemplate<String, LibreventsMessage> kafkaTemplate;

    private final KafkaSettings kafkaSettings;

    public KafkaLibreventsEventBroadcaster(
            KafkaTemplate<String, LibreventsMessage> kafkaTemplate, KafkaSettings kafkaSettings) {
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaSettings = kafkaSettings;
    }

    @Override
    public void broadcastEventFilterAdded(ContractEventFilter filter) {
        sendMessage(createContractEventFilterAddedMessage(filter));
    }

    @Override
    public void broadcastEventFilterRemoved(ContractEventFilter filter) {
        sendMessage(createContractEventFilterRemovedMessage(filter));
    }

    @Override
    public void broadcastTransactionMonitorAdded(TransactionMonitoringSpec spec) {
        sendMessage(createTransactionMonitorAddedMessage(spec));
    }

    @Override
    public void broadcastTransactionMonitorRemoved(TransactionMonitoringSpec spec) {
        sendMessage(createTransactionMonitorRemovedMessage(spec));
    }

    protected LibreventsMessage createContractEventFilterAddedMessage(ContractEventFilter filter) {
        return new ContractEventFilterAdded(filter);
    }

    protected LibreventsMessage createContractEventFilterRemovedMessage(
            ContractEventFilter filter) {
        return new ContractEventFilterRemoved(filter);
    }

    protected LibreventsMessage createTransactionMonitorAddedMessage(
            TransactionMonitoringSpec spec) {
        return new TransactionMonitorAdded(spec);
    }

    protected LibreventsMessage createTransactionMonitorRemovedMessage(
            TransactionMonitoringSpec spec) {
        return new TransactionMonitorRemoved(spec);
    }

    private void sendMessage(LibreventsMessage message) {
        log.info("Sending message: {}", JSON.stringify(message));
        kafkaTemplate.send(kafkaSettings.getEventeumEventsTopic(), message.getId(), message);
    }
}
