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
import io.librevents.integration.eventstore.SaveableEventStore;
import io.librevents.monitoring.LibreventsValueMonitor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(1)
@ConditionalOnBean(EventStoreFactory.class)
public class CustomEventStoreConfiguration {

    @Bean
    public SaveableEventStore customEventStore(EventStoreFactory factory) {
        return factory.build();
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
