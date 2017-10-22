package ai.api.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class FatResponseMessage {

    int type;
    String speech;
    String imageUrl;
    String title;
    String subtitle;
    String text;
    String postback;
    List<String> replies;
    Map<String, String> payload;
}
