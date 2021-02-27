package z.zaifapiclient.cashtxapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class IdInfo {
    String id;
    String email;
    String name;
    String kana;
    @JsonProperty(value = "certified")
    boolean certified;
}
