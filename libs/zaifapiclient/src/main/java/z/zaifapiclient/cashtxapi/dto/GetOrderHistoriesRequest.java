package z.zaifapiclient.cashtxapi.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetOrderHistoriesRequest {
    String method = "trade_history";
    Integer from;
    Integer count;
    Integer fromId;
    Integer endId;
    Order order;
    Long since;
    Long end;
    String currencyPair;
    Boolean isToken;

    public enum Order {
        ASC,
        DESC,
        ;
    }
}
