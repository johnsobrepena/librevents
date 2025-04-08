package io.librevents.utils;

import io.librevents.constant.Constants;
import io.librevents.model.TransactionIdentifierType;
import io.librevents.model.TransactionMonitoringSpec;
import io.librevents.service.TransactionMonitoringService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TransactionMonitorCreator {

    private final TransactionMonitoringService service;

    @Autowired
    public TransactionMonitorCreator(TransactionMonitoringService service) {
        this.service = service;
    }

    public void createTransactionMonitor(TransactionMonitoringSpec spec) {
        service.registerTransactionsToMonitor(spec);
    }

    public static TransactionMonitoringSpec buildTransactionMonitor(
            TransactionIdentifierType identifierType, String transactionHash) {
        return new TransactionMonitoringSpec(
                identifierType, transactionHash, Constants.DEFAULT_NODE_NAME);
    }
}
