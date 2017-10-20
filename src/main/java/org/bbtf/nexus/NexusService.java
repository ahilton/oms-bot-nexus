package org.bbtf.nexus;

import feign.Feign;
import org.bbtf.client.BotloggerClient;
import org.bbtf.model.PushEvent;
import org.bttf.v3client.ApiClient;
import org.bttf.v3client.ApiException;
import org.bttf.v3client.api.ConversationsApi;
import org.bttf.v3client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;

@Service
@Controller
public class NexusService {

    //private ConversationsApi conversationsApi;
    private ChannelAccount serviceAccount;
    private BotloggerClient botloggerService;

    private static String API_KEY = System.getProperty("bot.api.key");
    private static String BOT_LOGGER_HOST = System.getProperty("bot.logger.host");

    private static Logger logger = LoggerFactory.getLogger(NexusService.class);

    @PostConstruct
    public void init() throws ApiException {
        //conversationsApi = initApiClient(API_KEY);

        serviceAccount = new ChannelAccount();
        serviceAccount.setName("Nexus");
        serviceAccount.setId("nexus");

        logger.info("Initialising Botlogger feign client at host: "+BOT_LOGGER_HOST);
        botloggerService = Feign.builder()
                .target(BotloggerClient.class, BOT_LOGGER_HOST);

        logger.info("Initialisation complete");

    }

    private ConversationsApi initApiClient(String apiKey) {
        logger.info("Initialising conversation api");
        ApiClient apiClient = new ApiClient();
        apiClient.addDefaultHeader("Authorization", "Bearer "+apiKey);
        return new ConversationsApi(apiClient);
    }

    @CrossOrigin
    @PutMapping("/event/push")
    @ResponseBody
    public void pushEvent(@RequestBody PushEvent pushEvent) throws ApiException {
        ConversationsApi conversationsApi = initApiClient(pushEvent.getChannelKey());
        //Conversation conversation = conversationsApi.conversationsReconnectToConversation(pushEvent.getConversationId(), null);
        Activity activity = buildActivityForMessage(pushEvent);
        ResourceResponse resourceResponse = conversationsApi.conversationsPostActivity(pushEvent.getConversationId(), activity);
        logger.info(resourceResponse.toString());
    }

    private Activity buildActivityForMessage(PushEvent event) {
        Activity activity = new Activity();
        activity.setType("Message");
        activity.setText(event.getMessage());
        ChannelAccount account = new ChannelAccount();
        account.setId(event.getUserId());
        account.setName(event.getUserName());
        activity.setFrom(account);
        return activity;
    }
}
