package org.bbtf.nexus;

import org.bttf.v3client.ApiClient;
import org.bttf.v3client.ApiException;
import org.bttf.v3client.Configuration;
import org.bttf.v3client.api.ConversationsApi;
import org.bttf.v3client.api.TokensApi;
import org.bttf.v3client.model.Activity;
import org.bttf.v3client.model.ActivitySet;
import org.bttf.v3client.model.ChannelAccount;
import org.bttf.v3client.model.Conversation;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;


@Service
public class NexusService {

    private ConversationsApi conversationsApi;
    private TokensApi tokensApi;

    private static String API_KEY = System.getProperty("bot.api.key");

//    @PostConstruct
//    public void init(){
//        ApiClient apiClient = new ApiClient();
//        ApiKeyAuth auth = new ApiKeyAuth("header", "Bearer");
//        auth.setApiKey("a4a7xm8u_AU.cwA.tVM.UL9WULGsuPfS8fNGGkd8kDz6YIA410WQXcStF97qPYw");
//        apiClient.addAuthorization("Bearer",auth);
//        conversationsApi = apiClient.buildClient(ConversationsApi.class);
//        tokensApi = apiClient.buildClient(TokensApi.class);
//        converse();
//    }
//
//    @PostConstruct
//    public void init() throws ApiException {
//        //http://voicebase.readthedocs.io/en/v3/how-to-guides/swagger-codegen.html
//        ApiClient apiClient = Configuration.getDefaultApiClient();
//        Client httpClient = apiClient.getHttpClient();
//        //httpClient.property(ClientProperties.REQUEST_ENTITY_PROCESSING, RequestEntityProcessing.CHUNKED);
//        //httpClient.property(ClientProperties.CHUNKED_ENCODING_SIZE, ClientProperties.DEFAULT_CHUNK_SIZE);
////        ClientConfig configuration = new ClientConfig();
////        configuration.property();
//        apiClient.addDefaultHeader("Authorization", "Bearer "+API_KEY);
//        conversationsApi = new ConversationsApi();
//
//        converse();
//    }

    @PostConstruct
    public void init() throws ApiException {

        ApiClient defaultApiClient = Configuration.getDefaultApiClient();
        defaultApiClient.addDefaultHeader("Authorization", "Bearer "+API_KEY);

        conversationsApi = new ConversationsApi();

        converse();
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
        ChannelAccount cAccount = new ChannelAccount();
        cAccount.setName("AlexName");
        cAccount.setId("AlexId");
        activity.setFrom(cAccount);
        conversationsApi.conversationsPostActivity(conversation.getConversationId(), activity);

        while(true){
            ActivitySet activitySet = conversationsApi.conversationsGetActivities(conversation.getConversationId(), null);
            System.out.println(activitySet.toString());
        }

        //ActivitySet activitySet = conversationsApi.conversationsGetActivities();
        //System.out.println(activitySet);

    }
}
