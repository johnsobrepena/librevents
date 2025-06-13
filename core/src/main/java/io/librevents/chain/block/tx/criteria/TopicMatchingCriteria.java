package io.librevents.chain.block.tx.criteria;

import java.util.List;

import io.librevents.dto.transaction.TransactionDetails;
import io.librevents.dto.transaction.TransactionStatus;

public class TopicMatchingCriteria extends SingleValueMatchingCriteria<String> {

    public TopicMatchingCriteria(
            String nodeName, String fromAddress, List<TransactionStatus> statuses) {
        super(nodeName, fromAddress, statuses);
    }

    @Override
    public boolean isOneTimeMatch() {
        return false;
    }

    @Override
    protected String getValueFromTx(TransactionDetails tx) {
        return null;
    }
}
