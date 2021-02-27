package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetUnfilledOrderRequest {
    String method = "active_orders";
    String currencyPair;
    Boolean isToken;
    Boolean isTokenBoth;
}
