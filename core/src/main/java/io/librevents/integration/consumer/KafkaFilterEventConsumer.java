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

package io.librevents.integration.consumer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import io.librevents.dto.event.filter.ContractEventFilter;
import io.librevents.dto.message.*;
import io.librevents.model.TransactionMonitoringSpec;
import io.librevents.service.SubscriptionService;
import io.librevents.service.TransactionMonitoringService;
import io.librevents.service.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;

/**
 * A FilterEventConsumer that consumes ContractFilterEvents messages from a Kafka topic.
 *
 * <p>The topic to be consumed from can be configured via the kafka.topic.contractEvents property.
 *
 * @author Craig Williams craig.williams@consensys.net
 */
@Slf4j
public class KafkaFilterEventConsumer implements LibreventsInternalEventConsumer {

    private final Map<String, Consumer<LibreventsMessage>> messageConsumers;

    @Autowired
    public KafkaFilterEventConsumer(
            SubscriptionService subscriptionService,
            TransactionMonitoringService transactionMonitoringService) {

        messageConsumers = new HashMap<>();
        messageConsumers.put(
                ContractEventFilterAdded.TYPE,
                message ->
                        subscriptionService.registerContractEventFilter(
                                (ContractEventFilter) message.getDetails(), false));

        messageConsumers.put(
                ContractEventFilterRemoved.TYPE,
                message -> {
                    try {
                        subscriptionService.unregisterContractEventFilter(
                                ((ContractEventFilter) message.getDetails()).getId(), false);
                    } catch (NotFoundException e) {
                        log.debug(
                                "Received filter removed message but filter doesn't exist. (We probably sent message)");
                    }
                });

        messageConsumers.put(
                TransactionMonitorAdded.TYPE,
                message ->
                        transactionMonitoringService.registerTransactionsToMonitor(
                                (TransactionMonitoringSpec) message.getDetails(), false));

        messageConsumers.put(
                TransactionMonitorRemoved.TYPE,
                message -> {
                    try {
                        transactionMonitoringService.stopMonitoringTransactions(
                                ((TransactionMonitoringSpec) message.getDetails()).getId(), false);
                    } catch (NotFoundException e) {
                        log.debug(
                                "Received transaction monitor removed message but monitor doesn't exist. (We probably sent message)");
                    }
                });
    }

    @Override
    @KafkaListener(
            topics = "#{eventeumKafkaSettings.eventeumEventsTopic}",
            groupId = "#{eventeumKafkaSettings.groupId}",
            containerFactory = "eventeumKafkaListenerContainerFactory")
    @RetryableTopic(
            kafkaTemplate = "eventeumKafkaTemplate",
            listenerContainerFactory = "eventeumKafkaListenerContainerFactory",
            attempts = "${kafka.retries:10}",
            backoff = @Backoff(delayExpression = "${kafka.retry.backoff.msConfig:500}"),
            numPartitions = "${kafka.topic.partitions:3}",
            replicationFactor = "${kafka.topic.replicationSets:1}")
    public void onMessage(LibreventsMessage message) {
        final Consumer<LibreventsMessage> consumer = messageConsumers.get(message.getType());

        if (consumer == null) {
            log.error("No consumer for message type {}", message.getType());
            return;
        }

        consumer.accept(message);
    }
}
