package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NonNull;

@Data
@Builder
public class TradeRequest {
    String method = "trade";
    @NonNull String currencyPair;
    @NonNull String action;
    float price;
    float amount;
    Float limit;
    String comment;
}
