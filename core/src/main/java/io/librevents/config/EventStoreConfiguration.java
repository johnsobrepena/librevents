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

import io.librevents.chain.block.BlockListener;
import io.librevents.chain.block.EventStoreLatestBlockUpdater;
import io.librevents.chain.contract.ContractEventListener;
import io.librevents.chain.contract.EventStoreContractEventUpdater;
import io.librevents.chain.factory.BlockDetailsFactory;
import io.librevents.chain.service.container.ChainServicesContainer;
import io.librevents.factory.EventStoreFactory;
import io.librevents.integration.eventstore.EventStore;
import io.librevents.integration.eventstore.SaveableEventStore;
import io.librevents.integration.eventstore.db.MongoEventStore;
import io.librevents.integration.eventstore.db.SqlEventStore;
import io.librevents.integration.eventstore.db.repository.ContractEventDetailsRepository;
import io.librevents.integration.eventstore.db.repository.LatestBlockRepository;
import io.librevents.integration.eventstore.db.repository.MessageDetailsRepository;
import io.librevents.integration.eventstore.rest.RESTEventStore;
import io.librevents.integration.eventstore.rest.client.EventStoreClient;
import io.librevents.monitoring.LibreventsValueMonitor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
@Order(0)
public class EventStoreConfiguration {

    EventStoreConfiguration() {}

    @Configuration
    @ConditionalOnExpression("'${eventStore.type}:${database.type}'=='DB:MONGO'")
    @ConditionalOnMissingBean(EventStoreFactory.class)
    public static class MongoEventStoreConfiguration {

        @Bean
        public SaveableEventStore dbEventStore(
                ContractEventDetailsRepository contractEventRepository,
                MessageDetailsRepository messageDetailsRepository,
                LatestBlockRepository latestBlockRepository,
                MongoTemplate mongoTemplate) {
            return new MongoEventStore(
                    contractEventRepository,
                    messageDetailsRepository,
                    latestBlockRepository,
                    mongoTemplate);
        }

        @Bean
        public ContractEventListener eventStoreContractEventUpdater(SaveableEventStore eventStore) {
            return new EventStoreContractEventUpdater(eventStore);
        }

        @Bean
        public BlockListener eventStoreLatestBlockUpdater(
                SaveableEventStore eventStore,
                BlockDetailsFactory blockDetailsFactory,
                LibreventsValueMonitor valueMonitor,
                ChainServicesContainer chainServicesContainer) {
            return new EventStoreLatestBlockUpdater(
                    eventStore, blockDetailsFactory, valueMonitor, chainServicesContainer);
        }
    }

    @Configuration
    @ConditionalOnExpression("'${eventStore.type}:${database.type}'=='DB:SQL'")
    @ConditionalOnMissingBean(EventStoreFactory.class)
    public static class SqlEventStoreConfiguration {

        @Bean
        public SaveableEventStore dbEventStore(
                ContractEventDetailsRepository contractEventRepository,
                MessageDetailsRepository messageDetailsRepository,
                LatestBlockRepository latestBlockRepository) {
            return new SqlEventStore(
                    contractEventRepository, messageDetailsRepository, latestBlockRepository);
        }

        @Bean
        public ContractEventListener eventStoreContractEventUpdater(SaveableEventStore eventStore) {
            return new EventStoreContractEventUpdater(eventStore);
        }

        @Bean
        public BlockListener eventStoreLatestBlockUpdater(
                SaveableEventStore eventStore,
                BlockDetailsFactory blockDetailsFactory,
                LibreventsValueMonitor valueMonitor,
                ChainServicesContainer chainServiceContainer) {
            return new EventStoreLatestBlockUpdater(
                    eventStore, blockDetailsFactory, valueMonitor, chainServiceContainer);
        }
    }

    @Configuration
    @ConditionalOnProperty(name = "eventStore.type", havingValue = "REST")
    @ConditionalOnMissingBean(EventStoreFactory.class)
    public static class RESTEventStoreConfiguration {

        @Bean
        public EventStore restEventStore(EventStoreClient client) {
            return new RESTEventStore(client);
        }
    }
}
