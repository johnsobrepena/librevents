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

package io.librevents.chain.service.health;

import java.util.concurrent.ScheduledThreadPoolExecutor;

import io.librevents.chain.service.BlockchainException;
import io.librevents.chain.service.BlockchainService;
import io.librevents.chain.service.health.strategy.ReconnectionStrategy;
import io.librevents.chain.service.strategy.BlockSubscriptionStrategy;
import io.librevents.monitoring.LibreventsValueMonitor;
import io.librevents.service.EventStoreService;
import io.librevents.service.SubscriptionService;
import org.web3j.protocol.Web3jService;
import org.web3j.protocol.websocket.LibreventsWebSocketService;
import org.web3j.protocol.websocket.WebSocketClient;

public class WebSocketHealthCheckService extends NodeHealthCheckService {

    private final WebSocketClient webSocketClient;

    public WebSocketHealthCheckService(
            Web3jService web3jService,
            BlockchainService blockchainService,
            BlockSubscriptionStrategy blockSubscription,
            ReconnectionStrategy failureListener,
            SubscriptionService subscriptionService,
            LibreventsValueMonitor valueMonitor,
            EventStoreService eventStoreService,
            Integer syncingThreshold,
            ScheduledThreadPoolExecutor taskScheduler,
            Long healthCheckPollInterval) {
        super(
                blockchainService,
                blockSubscription,
                failureListener,
                subscriptionService,
                valueMonitor,
                eventStoreService,
                syncingThreshold,
                taskScheduler,
                healthCheckPollInterval);

        if (web3jService instanceof LibreventsWebSocketService libreventsWebSocketService) {
            this.webSocketClient = libreventsWebSocketService.getWebSocketClient();
        } else {
            throw new BlockchainException(
                    "Non web socket service passed to WebSocketHealthCheckService");
        }
    }

    @Override
    protected boolean isSubscribed() {
        return super.isSubscribed() && webSocketClient.isOpen();
    }
}
