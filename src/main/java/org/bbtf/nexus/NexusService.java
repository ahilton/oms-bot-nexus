package org.bbtf.nexus;

import feign.Feign;
import org.bbtf.client.BotloggerClient;
import org.bttf.v3client.ApiClient;
import org.bttf.v3client.ApiException;
import org.bttf.v3client.Configuration;
import org.bttf.v3client.api.ConversationsApi;
import org.bttf.v3client.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.annotation.PostConstruct;

@Service
@Controller
public class NexusService {

    private ConversationsApi conversationsApi;
    private ChannelAccount serviceAccount;
    private BotloggerClient botloggerService;

    private static String API_KEY = System.getProperty("bot.api.key");
    private static String BOT_LOGGER_HOST = System.getProperty("bot.logger.host");

    private static Logger logger = LoggerFactory.getLogger(NexusService.class);

    @PostConstruct
    public void init() throws ApiException {
        logger.info("Initialising conversation api");
        ApiClient defaultApiClient = Configuration.getDefaultApiClient();
        defaultApiClient.addDefaultHeader("Authorization", "Bearer "+API_KEY);
        conversationsApi = new ConversationsApi();

        ChannelAccount cAccount = new ChannelAccount();
        cAccount.setName("Nexus");
        cAccount.setId("nexus");

        logger.info("Initialising Botlogger feign client at host: "+BOT_LOGGER_HOST);
        botloggerService = Feign.builder()
                .target(BotloggerClient.class, BOT_LOGGER_HOST);

        logger.info("Initialisation complete");

    }

    @GetMapping("/conversation/events")
    @ResponseBody
    public ActivitySet getConversationEvents(
            @RequestParam(name="conversationId", required=false) String conversationId,
            @RequestParam(name="watermark", required=false) String watermark) throws ApiException {

        if (conversationId == null || conversationId.isEmpty()){
            logger.info("Looking up latest conversation id...");
            conversationId = botloggerService.getLastConversationId();
            logger.info("Using latest conversation id:"+conversationId);
        }
        //conversationsApi.conversationsReconnectToConversation()
        //Conversation conversation = conversationsApi.conversationsReconnectToConversation(conversationId, watermark);
        return conversationsApi.conversationsGetActivities(conversationId, watermark);
    }

    @GetMapping("/conversation/new")
    @ResponseBody
    public String startConversation(@RequestBody String initialMessage) throws ApiException {
        Conversation conversation = conversationsApi.conversationsStartConversation();
        String conversationId = conversation.getConversationId();
        Activity activity = buildActivityForMessage(initialMessage);
        conversationsApi.conversationsPostActivity(conversationId, activity);
        return conversationId;
    }

    private Activity buildActivityForMessage(String initialMessage) {
        Activity activity = new Activity();
        activity.setType("message");
        activity.setText(initialMessage);
        activity.setFrom(serviceAccount);
        return activity;
    }

    private void converse() throws ApiException {
//        ChannelAccount user = new ChannelAccount();
//        user.setId("a4a7xm8u_AU.cwA.tVM.UL9WULGsuPfS8fNGGkd8kDz6YIA410WQXcStF97qPYw");
//        TokenParameters tokenParams = new TokenParameters();
        //tokenParams.setETag("a4a7xm8u_AU.cwA.tVM.UL9WULGsuPfS8fNGGkd8kDz6YIA410WQXcStF97qPYw");
        //tokenParams.setUser(user);
        //conversationsApi.conversationsStartConversation();
        //Conversation conversation = tokensApi.tokensGenerateTokenForNewConversation(tokenParams);
        Conversation conversation = conversationsApi.conversationsStartConversation();
        System.out.println(conversation);

        Activity activity = new Activity();
        activity.setType("message");
        activity.setText("order 1000 shares of appl");

        activity.setFrom(serviceAccount);
        conversationsApi.conversationsPostActivity(conversation.getConversationId(), activity);

//        while(true){
            ActivitySet activitySet = conversationsApi.conversationsGetActivities(conversation.getConversationId(), null);
            System.out.println(activitySet.toString());
//        }

        //ActivitySet activitySet = conversationsApi.conversationsGetActivities();
        //System.out.println(activitySet);

    }
}
