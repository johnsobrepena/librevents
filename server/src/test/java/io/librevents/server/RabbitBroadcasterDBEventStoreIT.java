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

package io.librevents.server;

import java.util.Collections;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.librevents.dto.block.BlockDetails;
import io.librevents.dto.event.ContractEventDetails;
import io.librevents.dto.message.LibreventsMessage;
import io.librevents.dto.transaction.TransactionDetails;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(locations = "classpath:application-test-db-rabbit.properties")
public class RabbitBroadcasterDBEventStoreIT extends BroadcasterSmokeTest {

    @Autowired private ConnectionFactory connectionFactory;

    @Autowired private ObjectMapper objectMapper;

    @BeforeAll
    public static void startRabbitContainer() {
        RabbitMQContainer rabbitContainer =
                new RabbitMQContainer("rabbitmq:3.7.25-management-alpine");
        rabbitContainer.withExchange("ThisIsAExchange", "topic");
        rabbitContainer.withQueue("contractEvents");
        rabbitContainer.withQueue("transactionEvents");
        rabbitContainer.withQueue("blockEvents");
        rabbitContainer.withBinding(
                "ThisIsAExchange",
                "contractEvents",
                Collections.emptyMap(),
                "contractEvents.#",
                "queue");
        rabbitContainer.withBinding(
                "ThisIsAExchange",
                "transactionEvents",
                Collections.emptyMap(),
                "transactionEvents.#",
                "queue");
        rabbitContainer.withBinding(
                "ThisIsAExchange", "blockEvents", Collections.emptyMap(), "blockEvents.#", "queue");
        rabbitContainer.start();
        rabbitContainer.waitingFor(Wait.defaultWaitStrategy());

        System.setProperty("RABBITMQ_HOST", rabbitContainer.getHost());
        System.setProperty("RABBITMQ_PORT", String.valueOf(rabbitContainer.getAmqpPort()));
    }

    @BeforeEach
    public void setup() {
        createListener(
                "contractEvents",
                message -> {
                    if (message.getDetails() instanceof ContractEventDetails) {
                        onContractEventMessageReceived((ContractEventDetails) message.getDetails());
                    } else if (message.getDetails() instanceof TransactionDetails) {
                        onTransactionMessageReceived((TransactionDetails) message.getDetails());
                    }
                });

        createListener(
                "transactionEvents",
                message -> {
                    onTransactionMessageReceived((TransactionDetails) message.getDetails());
                });

        createListener(
                "blockEvents",
                message -> {
                    onBlockMessageReceived((BlockDetails) message.getDetails());
                });
    }

    private void createListener(
            String queueName, java.util.function.Consumer<LibreventsMessage> eventHandler) {
        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.setQueueNames(queueName);

        MessageListenerAdapter adapter =
                new MessageListenerAdapter(
                        new Object() {
                            public void handleMessage(byte[] body) {
                                try {
                                    LibreventsMessage message =
                                            objectMapper.readValue(body, LibreventsMessage.class);
                                    eventHandler.accept(message);
                                } catch (Exception ignored) {
                                }
                            }
                        });

        adapter.setDefaultListenerMethod("handleMessage");
        container.setMessageListener(adapter);
        container.start();
    }
}
