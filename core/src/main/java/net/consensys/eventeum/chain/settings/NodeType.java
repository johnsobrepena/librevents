package net.consensys.eventeum.chain.settings;

import lombok.Getter;

@Getter
public enum NodeType {
    NORMAL("NORMAL"),
    MIRROR("MIRROR");

    private final String nodeName;

    NodeType(String nodeName) {
        this.nodeName = nodeName;
    }
}
