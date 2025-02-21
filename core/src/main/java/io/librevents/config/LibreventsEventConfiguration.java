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

package io.librevents.config;

import io.librevents.dto.message.LibreventsMessage;
import io.librevents.integration.KafkaSettings;
import io.librevents.integration.broadcast.internal.DoNothingLibreventsEventBroadcaster;
import io.librevents.integration.broadcast.internal.KafkaLibreventsEventBroadcaster;
import io.librevents.integration.broadcast.internal.LibreventsEventBroadcaster;
import io.librevents.integration.consumer.KafkaFilterEventConsumer;
import io.librevents.integration.consumer.LibreventsInternalEventConsumer;
import io.librevents.service.SubscriptionService;
import io.librevents.service.TransactionMonitoringService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Spring bean configuration for the FilterEvent broadcaster and consumer.
 *
 * <p>If broadcaster.multiInstance is set to true, then register a Kafka broadcaster, otherwise
 * register a dummy broadcaster that does nothing.
 *
 * @author Craig Williams craig.williams@consensys.net
 */
@Configuration
public class LibreventsEventConfiguration {

    @Bean
    @ConditionalOnProperty(name = "broadcaster.multiInstance", havingValue = "true")
    public LibreventsEventBroadcaster kafkaFilterEventBroadcaster(
            KafkaTemplate<String, LibreventsMessage> kafkaTemplate, KafkaSettings kafkaSettings) {
        return new KafkaLibreventsEventBroadcaster(kafkaTemplate, kafkaSettings);
    }

    @Bean
    @ConditionalOnProperty(name = "broadcaster.multiInstance", havingValue = "true")
    public LibreventsInternalEventConsumer kafkaFilterEventConsumer(
            SubscriptionService subscriptionService,
            TransactionMonitoringService transactionMonitoringService) {
        return new KafkaFilterEventConsumer(subscriptionService, transactionMonitoringService);
    }

    @Bean
    @ConditionalOnProperty(
            name = "broadcaster.multiInstance",
            havingValue = "false",
            matchIfMissing = true)
    public LibreventsEventBroadcaster doNothingFilterEventBroadcaster() {
        return new DoNothingLibreventsEventBroadcaster();
    }
}
