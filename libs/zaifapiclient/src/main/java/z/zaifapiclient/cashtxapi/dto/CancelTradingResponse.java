package z.zaifapiclient.cashtxapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class CancelTradingResponse {
    int orderId;
    Map<String, Float> funds;
}
