package io.librevents.privacy.chain.block.tx;

import java.util.List;
import java.util.Optional;

import io.librevents.chain.block.tx.DefaultTransactionMonitoringBlockListener;
import io.librevents.chain.block.tx.criteria.TransactionMatchingCriteria;
import io.librevents.chain.factory.TransactionDetailsFactory;
import io.librevents.chain.service.block.BlockCache;
import io.librevents.chain.service.container.ChainServicesContainer;
import io.librevents.chain.service.domain.Block;
import io.librevents.chain.service.domain.Transaction;
import io.librevents.chain.settings.Node;
import io.librevents.chain.settings.NodeSettings;
import io.librevents.dto.transaction.TransactionDetails;
import io.librevents.dto.transaction.TransactionStatus;
import io.librevents.integration.broadcast.blockchain.BlockchainEventBroadcaster;
import io.librevents.privacy.chain.model.PrivacyConfNode;
import io.librevents.privacy.chain.service.Web3JEeaService;
import io.librevents.privacy.chain.util.PrivacyUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
public class EeaTransactionMonitoringBlockListener
        extends DefaultTransactionMonitoringBlockListener {

    private final NodeSettings nodeSettings;
    private final ChainServicesContainer chainServicesContainer;
    private final TransactionDetailsFactory transactionDetailsFactory;

    public EeaTransactionMonitoringBlockListener(
            ChainServicesContainer chainServicesContainer,
            BlockchainEventBroadcaster broadcaster,
            TransactionDetailsFactory transactionDetailsFactory,
            BlockCache blockCache,
            NodeSettings nodeSettings) {
        super(
                chainServicesContainer,
                broadcaster,
                transactionDetailsFactory,
                blockCache,
                nodeSettings);
        this.nodeSettings = nodeSettings;
        this.chainServicesContainer = chainServicesContainer;
        this.transactionDetailsFactory = transactionDetailsFactory;
    }

    @Override
    protected void broadcastIfMatched(
            Transaction tx, Block block, List<TransactionMatchingCriteria> criteriaToCheck) {
        Node node = nodeSettings.getNode(block.getNodeName());
        PrivacyConfNode privacyConf =
                PrivacyUtils.buildPrivacyConfNodeFromExtension(node.getExtension());

        // If node has privacy enabled
        if (privacyConf.isEnabled()) {
            Web3JEeaService eeaInstance =
                    (Web3JEeaService)
                            chainServicesContainer
                                    .getNodeServices(block.getNodeName())
                                    .getBlockchainService();
            if (eeaInstance.isPrivateTransaction(tx)) {
                Optional.ofNullable(eeaInstance.getPrivateTransactionReceipt(tx.getHash()))
                        .ifPresent(
                                txReceipt -> {
                                    final TransactionDetails txDetails =
                                            transactionDetailsFactory.createTransactionDetails(
                                                    tx,
                                                    txReceipt.getStatus().equals("0x1")
                                                            ? TransactionStatus.CONFIRMED
                                                            : TransactionStatus.FAILED,
                                                    block);
                                    txDetails.setTo(txReceipt.getTo());
                                    txDetails.setRevertReason(
                                            Boolean.TRUE.equals(
                                                            node.getAddTransactionRevertReason())
                                                    ? txReceipt.getRevertReason()
                                                    : null);
                                    criteriaToCheck.stream()
                                            .filter(matcher -> matcher.isAMatch(txDetails))
                                            .findFirst()
                                            .ifPresent(
                                                    matcher ->
                                                            onTransactionMatched(
                                                                    txDetails, matcher));
                                });
                return;
            }
        }
        super.broadcastIfMatched(tx, block, criteriaToCheck);
    }
}
