package z.zaifapiclient.cashtxapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TradeResponse {
    float received;
    float remains;
    int orderId;
    Map<String, Float> funds;
}
