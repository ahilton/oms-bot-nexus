package org.bbtf.nexus;

import ai.api.model.*;
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

    private static String API_KEY = System.getProperty("bot.api.key", "__a4a7xm8u_AU.cwA.tVM.UL9WULGsuPfS8fNGGkd8kDz6YIA410WQXcStF97qPYw");
    private static String BOT_LOGGER_HOST = System.getProperty("bot.logger.host");

    private static Logger logger = LoggerFactory.getLogger(NexusService.class);

    @PostConstruct
    public void init() throws ApiException {
        //conversationsApi = initApiClient(API_KEY);

        serviceAccount = new ChannelAccount();
        serviceAccount.setName("Nexus");
        serviceAccount.setId("nexus");

//        logger.info("Initialising Botlogger feign client at host: "+BOT_LOGGER_HOST);
//        botloggerService = Feign.builder()
//                .target(BotloggerClient.class, BOT_LOGGER_HOST);

        logger.info("Initialisation complete");

    }

    private ConversationsApi initApiClient(String apiKey) {
        logger.info("Initialising conversation api");
        ApiClient apiClient = new ApiClient();
        apiClient.addDefaultHeader("Authorization", "Bearer "+apiKey);
        return new ConversationsApi(apiClient);
    }

    /**
     * Web-hook request model class
     */
    protected static class AIWebhookRequest extends AIResponse {
        private static final long serialVersionUID = 1L;

        private AIOriginalRequest originalRequest;

        /**
         * Get original request object
         * @return <code>null</code> if original request undefined in
         * request object
         */
        public AIOriginalRequest getOriginalRequest() {
            return originalRequest;
        }
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

    @CrossOrigin
    @PostMapping("/event/client/v2")
    @ResponseBody
    public Fulfillment clientEvent(@RequestBody AIWebhookRequest response){
        logger.info("Client event triggered");
        logger.info(response.toString());
        Result result = response.getResult();
        String resolvedQuery = result.getResolvedQuery();
        logger.info("resolved query:"+resolvedQuery);
        AIOriginalRequest originalRequest = response.getOriginalRequest();
        logger.info("orig req: "+originalRequest.getData().toString());
        return testFulfillment();
    }

    private Fulfillment testFulfillment() {
        Fulfillment fulfillment = new Fulfillment();
        fulfillment.setSpeech("hello world from oms bot");
        fulfillment.setDisplayText("hello world from oms bot");
        fulfillment.setSource("omsgateway");
        return fulfillment;
    }

    @CrossOrigin
    @PostMapping("/event/client")
    @ResponseBody
    public Fulfillment clientEvent(@RequestBody AIResponse response){
        logger.info("Client event triggered");
        logger.info(response.toString());
        Result result = response.getResult();
        String resolvedQuery = result.getResolvedQuery();
        logger.info("resolved query:"+resolvedQuery);
        return testFulfillment();
    }

    @CrossOrigin
    @PostMapping("/event/test")
    @ResponseBody
    public Fulfillment clientEvent(@RequestBody AIRequest req){
        logger.info("Client req triggered");
        logger.info(req.toString());
        return testFulfillment();
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
