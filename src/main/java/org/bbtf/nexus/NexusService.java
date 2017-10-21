package org.bbtf.nexus;

import ai.api.model.*;
import org.bbtf.model.PushEvent;
import org.bttf.v3client.ApiClient;
import org.bttf.v3client.ApiException;
import org.bttf.v3client.api.ConversationsApi;
import org.bttf.v3client.model.*;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.Iterate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.util.Collection;
import java.util.List;

@Service
@Controller
public class NexusService {

    public static final String CLIENT_ERROR_REPLY = "Oh dear. Connection timeout waiting for bot to answer me. Say refresh to try again.";
    ConcurrentMutableMap<String, String> clientBotConversationMap = new ConcurrentHashMap<>();

    private ConversationsApi conversationsApi;
    private ChannelAccount serviceAccount;

    private static String API_KEY = System.getProperty("bot.api.key", "__a4a7xm8u_AU.cwA.tVM.UL9WULGsuPfS8fNGGkd8kDz6YIA410WQXcStF97qPYw");

    private static Logger logger = LoggerFactory.getLogger(NexusService.class);

    @PostConstruct
    public void init() throws ApiException {
        conversationsApi = initApiClient(API_KEY);

        serviceAccount = new ChannelAccount();
        serviceAccount.setName("Nexus");
        serviceAccount.setId("nexus");

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
        ChannelAccount account = new ChannelAccount();
        account.setId(pushEvent.getUserId());
        account.setName(pushEvent.getUserName());
        Activity activity = buildActivityForMessage(pushEvent.getMessage(), account);
        ResourceResponse resourceResponse = conversationsApi.conversationsPostActivity(pushEvent.getConversationId(), activity);
        logger.info(resourceResponse.toString());
    }

    @CrossOrigin
    @PostMapping("/event/client")
    @ResponseBody
    public Fulfillment clientEvent(@RequestBody AIWebhookRequest response) {
        logger.info("Client event triggered");
        logger.info(response.toString());
        Result result = response.getResult();
        String resolvedQuery = result.getResolvedQuery();
        logger.info("resolved query:"+resolvedQuery);
        AIOriginalRequest originalRequest = response.getOriginalRequest();
        logger.info("orig req: "+originalRequest.getData().toString());

        String botReply = null;
        try {
            botReply = submitToBot(response.getSessionId(), resolvedQuery);
        } catch (ApiException e) {
            logger.error("Exception trying to submit message to bot", e);
            botReply = CLIENT_ERROR_REPLY;
        }
        logger.info("Reply from bot:"+botReply);
        return buildFulfillment(botReply);
    }

    private String submitToBot(String clientConversationId, String text) throws ApiException {
        String serverConversationId;
        if (clientBotConversationMap.containsKey(clientConversationId)){
            logger.info("Reconnecting to client conversation id:"+clientConversationId);
            serverConversationId = clientBotConversationMap.get(clientConversationId);
            logger.info("Reusing server conversation id:"+serverConversationId);
        }
        else {
            logger.info("Starting new conversation for client conversation id:"+clientConversationId);
            Conversation conversation = conversationsApi.conversationsStartConversation();
            serverConversationId = conversation.getConversationId();
            logger.info("caching new server conversation id:"+serverConversationId);
            clientBotConversationMap.put(clientConversationId, serverConversationId);
        }
        //fetch water mark prior to sending activity...
        ActivitySet activitySet = conversationsApi.conversationsGetActivities(serverConversationId, null);
        String watermark = activitySet.getWatermark();
        logger.info("watermark:"+watermark);

        Activity activity = buildActivityForMessage(text, serviceAccount);
        ResourceResponse resourceResponse = conversationsApi.conversationsPostActivity(serverConversationId, activity);
        logger.info(resourceResponse.toString());

        int retryCount = 0;
        while(retryCount<100){
            ActivitySet updatedActivitySet = conversationsApi.conversationsGetActivities(serverConversationId, watermark);
            List<Activity> activities = updatedActivitySet.getActivities();
            Collection<Activity> x = Iterate.reject(activities, (a) -> a.getFrom().getId().equals(serviceAccount.getId()));
            if (x.isEmpty()){
                logger.info("Waiting for activity update... retry:"+retryCount);
                continue;
            }
            Activity last = Iterate.getLast(x);
            // return speech ahead of text
            if (last.getSpeak() != null){
                return last.getSpeak();
            }
            if (last.getText() != null){
                return last.getText();
            }
        }
        // wait for a reply
        return CLIENT_ERROR_REPLY;
    }

    // Microsoft
    private Activity buildActivityForMessage(String message, ChannelAccount serviceAccount) {
        Activity activity = new Activity();
        activity.setType("Message");
        activity.setText(message);
        activity.setFrom(serviceAccount);
        return activity;
    }

    /*
        GOOGLE API - Fulfillment carries response from bot back to dialog flow
     */
    private Fulfillment buildFulfillment(String message) {
        Fulfillment fulfillment = new Fulfillment();
        fulfillment.setSpeech(message);
        fulfillment.setDisplayText(message);
        fulfillment.setSource("omsgateway");
        return fulfillment;
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

    /*
        DEBUGGING
     */

    @CrossOrigin
    @GetMapping("/test/c")
    @ResponseBody
    public String testConversation() throws ApiException {
        return submitToBot("test-123", "order microsoft");
    }

    @CrossOrigin
    @GetMapping("/test/r")
    @ResponseBody
    public String testConversationReply() throws ApiException {
        return submitToBot("test-123", "5");
    }

    @CrossOrigin
    @GetMapping("/test/p")
    @ResponseBody
    public boolean testPing(){
        return true;
    }
}
