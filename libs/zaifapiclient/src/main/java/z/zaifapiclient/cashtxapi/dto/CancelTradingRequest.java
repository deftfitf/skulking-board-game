package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CancelTradingRequest {
    String method = "cancel_order";
    int orderId;
    String currencyPair;
    Boolean isToken;
}
