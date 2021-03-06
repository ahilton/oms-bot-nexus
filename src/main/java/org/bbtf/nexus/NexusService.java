package org.bbtf.nexus;

import ai.api.model.*;
import org.bbtf.model.PushEvent;
import org.bttf.v3client.ApiClient;
import org.bttf.v3client.ApiException;
import org.bttf.v3client.api.ConversationsApi;
import org.bttf.v3client.model.*;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.ConcurrentMutableMap;
import org.eclipse.collections.impl.factory.Lists;
import org.eclipse.collections.impl.map.mutable.ConcurrentHashMap;
import org.eclipse.collections.impl.utility.ArrayIterate;
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

    public static final String CLIENT_ERROR_REPLY = "Oh dear. Connection timeout waiting for bot to answer me.";
    public static final String CLIENT_UPDATE_ERROR_REPLY = "Looks like you have not started an order yet. Say order to get started.";
    ConcurrentMutableMap<String, String> clientBotConversationMap = new ConcurrentHashMap<>();

    private ConversationsApi conversationsApi;
    private ChannelAccount serviceAccount;

    private static String API_KEY = System.getProperty("bot.api.key", "__a4a7xm8u_AU.cwA.tVM.UL9WULGsuPfS8fNGGkd8kDz6YIA410WQXcStF97qPYw");

    private static String buyCorrections = System.getProperty("buy.corrections", "bye,bi,boy");
    private static String sellCorrections = System.getProperty("sell.corrections", "south,sal,selfies");
    private static String orderCorrections = System.getProperty("order.corrections", "border,bipolar");

    private static MutableList<String> buyCorrectionList = Lists.mutable.of();
    private static MutableList<String> sellCorrectionList = Lists.mutable.of();
    private static MutableList<String> orderCorrectionList = Lists.mutable.of();

    private static Logger logger = LoggerFactory.getLogger(NexusService.class);

    @PostConstruct
    public void init() throws ApiException {
        conversationsApi = initApiClient(API_KEY);

        serviceAccount = new ChannelAccount();
        serviceAccount.setName("Nexus");
        serviceAccount.setId("nexus");

        initCorrectionList(buyCorrectionList, "buy", buyCorrections);
        initCorrectionList(sellCorrectionList, "sell", sellCorrections);
        initCorrectionList(orderCorrectionList, "order", orderCorrections);

        logger.info("Initialisation complete");
    }

    private void initCorrectionList(MutableList<String> targetList, String type, String correctionCSV) {
        ArrayIterate.addAllTo(correctionCSV.split(","), targetList);
        logger.info(type+" corrections:"+targetList.toString());
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
        logger.info("Received push event:"+pushEvent.toString());
        ConversationsApi conversationsApi = pushEvent.getChannelKey()==null?this.conversationsApi:initApiClient(pushEvent.getChannelKey());
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
        String clientConversationId = response.getSessionId();

        if (resolvedQuery.equalsIgnoreCase("update") ||
                resolvedQuery.equalsIgnoreCase("refresh")){
            try {
                String serverConversationId = getServerConversationId(clientConversationId);
                String lastBotMessage = getLastBotMessage(serverConversationId, null);
                return buildFulfillment(lastBotMessage==null?CLIENT_UPDATE_ERROR_REPLY:lastBotMessage);
            } catch (ApiException e) {
                logger.error("Exception trying to submit message to bot", e);
                return buildFulfillment(CLIENT_UPDATE_ERROR_REPLY);
            }
        }

        resolvedQuery = autoCorrect(resolvedQuery);

        AIOriginalRequest originalRequest = response.getOriginalRequest();
        logger.info("orig req: "+originalRequest.getData().toString());

        String botReply = null;
        try {
            botReply = submitToBot(clientConversationId, resolvedQuery);
        } catch (ApiException e) {
            logger.error("Exception trying to submit message to bot", e);
            botReply = CLIENT_ERROR_REPLY;
        }
        logger.info("Reply from bot:"+botReply);
        return buildFulfillment(botReply);
    }

    private String autoCorrect(String resolvedQuery) {

        if (resolvedQuery.contains(" ")){
            //only apply autocorrect to single word sentences
            return resolvedQuery;
        }
        if (orderCorrectionList.contains(resolvedQuery.toLowerCase())){
            logger.info("Applying correction "+resolvedQuery+ "-> order");
            return "order";
        }
        if (buyCorrectionList.contains(resolvedQuery.toLowerCase())){
            logger.info("Applying correction "+resolvedQuery+ "-> buy");
            return "buy";
        }
        if (sellCorrectionList.contains(resolvedQuery.toLowerCase())){
            logger.info("Applying correction "+resolvedQuery+ "-> sell");
            return "sell";
        }
        return resolvedQuery;
    }

    private String submitToBot(String clientConversationId, String text) throws ApiException {
        String serverConversationId = getServerConversationId(clientConversationId);
        //fetch water mark prior to sending activity...
        ActivitySet activitySet = conversationsApi.conversationsGetActivities(serverConversationId, null);
        String watermark = activitySet.getWatermark();
        logger.info("watermark:"+watermark);

        Activity activity = buildActivityForMessage(text, serviceAccount);
        ResourceResponse resourceResponse = conversationsApi.conversationsPostActivity(serverConversationId, activity);
        logger.info(resourceResponse.toString());

        int retryCount = 0;
        while(retryCount<100){
            retryCount++;
            String lastMessage = getLastBotMessage(serverConversationId, watermark);
            if (lastMessage != null) {
                return lastMessage;
            }
            logger.info("Waiting for activity update... retry:"+retryCount);

        }
        // wait for a reply
        return CLIENT_ERROR_REPLY;
    }

    private String getServerConversationId(String clientConversationId) throws ApiException {
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
        return serverConversationId;
    }

    private String getLastBotMessage(String serverConversationId, String watermark) throws ApiException {
        ActivitySet updatedActivitySet = conversationsApi.conversationsGetActivities(serverConversationId, watermark);
        List<Activity> activities = updatedActivitySet.getActivities();
        Collection<Activity> botActivity = Iterate.reject(activities, (a) -> a.getFrom().getId().equals(serviceAccount.getId()));
        if (botActivity.isEmpty()){
            return null;
        }
        Activity last = Iterate.getLast(botActivity);
        // return speech ahead of text
        if (last.getSpeak() != null){
            return last.getSpeak();
        }
        if (last.getText() != null){
            return last.getText();
        }
        return null;
    }

    // Microsoft
    private Activity buildActivityForMessage(String message, ChannelAccount serviceAccount) {
        Activity activity = new Activity();
        activity.setType("message");
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
