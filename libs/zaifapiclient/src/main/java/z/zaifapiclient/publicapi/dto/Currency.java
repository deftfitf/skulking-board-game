package z.zaifapiclient.publicapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class Currency {
    int id;
    String name;
    @JsonProperty(value = "is_token")
    boolean isToken;
    String tokenId;
}
