package org.bbtf.client;

import feign.RequestLine;
import org.springframework.cloud.netflix.feign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient
public interface BotloggerClient {

    @RequestLine("GET /conversation/last")
    @ResponseBody
    public String getLastConversationId();
}
