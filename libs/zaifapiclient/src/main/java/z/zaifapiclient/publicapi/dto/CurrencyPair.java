package z.zaifapiclient.publicapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CurrencyPair {
    int id;
    String name;
    String title;
    String currencyPair;
    String description;
    @JsonProperty(value = "is_token")
    boolean isToken;
    int eventNumber;
    int seq;
    float itemUnitMin;
    float itemUnitStep;
    String itemJapanese;
    float auxUnitMin;
    float auxUnitStep;
    int auxUnitPoint;
    String auxJapanese;
}
