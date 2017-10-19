package org.bbtf.model;

import lombok.Data;

@Data
public class PushEvent {
    String message;
    String channelKey;
    String conversationId;
}
