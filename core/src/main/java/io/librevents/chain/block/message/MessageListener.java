package io.librevents.chain.block.message;

import io.librevents.dto.message.MessageDetails;

public interface MessageListener {

    void onMessage(MessageDetails message);
}
