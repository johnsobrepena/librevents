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

import java.math.BigInteger;

import io.librevents.dto.event.ContractEventDetails;
import io.librevents.dto.event.ContractEventStatus;
import io.librevents.dto.event.filter.ContractEventFilter;
import io.librevents.dto.transaction.TransactionDetails;
import io.librevents.dto.transaction.TransactionStatus;
import io.librevents.integration.broadcast.internal.KafkaLibreventsEventBroadcaster;
import io.librevents.model.TransactionIdentifierType;
import io.librevents.model.TransactionMonitoringSpec;
import io.librevents.repository.ContractEventFilterRepository;
import io.librevents.repository.TransactionMonitoringSpecRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.web3j.crypto.Hash;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@TestPropertySource(locations = "classpath:application-test-multiinstance.properties")
@TestMethodOrder(MethodOrderer.MethodName.class)
class LibreventsEventConsumingIT extends BaseKafkaIntegrationTest {

    @Autowired private KafkaLibreventsEventBroadcaster broadcaster;

    @Autowired private ContractEventFilterRepository filterRepo;

    @Autowired private TransactionMonitoringSpecRepository txMonitorRepo;

    @Test
    void testFilterAddedEventRegistersFilter() throws Exception {

        doBroadcastFilterAddedEventAndVerifyRegistered(deployEventEmitterContract());
    }

    @Test
    void testFilterRemovedEventRemovesFilter() throws Exception {
        final EventEmitter emitter = deployEventEmitterContract();

        final ContractEventFilter filter = doBroadcastFilterAddedEventAndVerifyRegistered(emitter);

        broadcaster.broadcastEventFilterRemoved(filter);

        waitForFilterEventMessages(2);

        clearMessages();

        emitter.emitEvent(stringToBytes("BytesValue"), BigInteger.TEN, "StringValue").send();

        waitForBroadcast();

        assertEquals(0, getBroadcastContractEvents().size());
    }

    @Test
    void testTxMonitorAddedEventRegistersMonitor() throws Exception {

        final String signedTxHex = createRawSignedTransactionHex();
        final String txHash = Hash.sha3(signedTxHex);

        final TransactionMonitoringSpec spec = new TransactionMonitoringSpec();
        spec.setNodeName("default");
        spec.setTransactionIdentifierValue(txHash);
        spec.setType(TransactionIdentifierType.HASH);

        broadcaster.broadcastTransactionMonitorAdded(spec);

        waitForTransactionMonitorEventMessages(1);

        assertEquals(txHash, sendRawTransaction(signedTxHex));

        waitForTransactionMessages(1);

        assertEquals(1, getBroadcastTransactionMessages().size());

        final TransactionDetails txDetails = getBroadcastTransactionMessages().getFirst();
        assertEquals(txHash, txDetails.getHash());
        assertEquals(TransactionStatus.UNCONFIRMED, txDetails.getStatus());
    }

    @Test
    void testTxMonitorRemovedEventRemovesMonitor() throws Exception {

        final String signedTxHex = createRawSignedTransactionHex();
        final String txHash = Hash.sha3(signedTxHex);

        final TransactionMonitoringSpec spec = new TransactionMonitoringSpec();
        spec.setNodeName("default");
        spec.setTransactionIdentifierValue(txHash);
        spec.setType(TransactionIdentifierType.HASH);

        broadcaster.broadcastTransactionMonitorAdded(spec);

        waitForTransactionMonitorEventMessages(1);

        broadcaster.broadcastTransactionMonitorRemoved(spec);

        waitForTransactionMonitorEventMessages(1);

        assertEquals(txHash, sendRawTransaction(signedTxHex));

        waitForBroadcast();

        assertEquals(0, getBroadcastTransactionMessages().size());
    }

    private ContractEventFilter doBroadcastFilterAddedEventAndVerifyRegistered(EventEmitter emitter)
            throws Exception {

        final ContractEventFilter filter = createDummyEventFilter(emitter.getContractAddress());

        broadcaster.broadcastEventFilterAdded(filter);

        waitForFilterEventMessages(1);

        emitter.emitEvent(stringToBytes("BytesValue"), BigInteger.TEN, "StringValue").send();

        waitForContractEventMessages(1);

        assertEquals(1, getBroadcastContractEvents().size());

        final ContractEventDetails eventDetails = getBroadcastContractEvents().getFirst();
        verifyDummyEventDetails(filter, eventDetails, ContractEventStatus.UNCONFIRMED);

        return filter;
    }

    private void waitForFilterEventMessages(int expectedMessageCounnt) throws InterruptedException {
        waitForMessages(expectedMessageCounnt, getBroadcastFilterEventMessages());

        // Wait an extra 2 seconds because there may be a race condition
        Thread.sleep(2000);
    }

    private void waitForTransactionMonitorEventMessages(int expectedMessageCounnt)
            throws InterruptedException {
        waitForMessages(expectedMessageCounnt, getBroadcastTransactionEventMessages());

        // Wait an extra 2 seconds because there may be a race condition
        Thread.sleep(2000);
    }
}
