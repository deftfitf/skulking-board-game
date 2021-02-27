package z.zaifapiclient.cashtxapi.dto;

import lombok.Data;

@Data
public class OrderHistory {
    String currencyPair;
    String action;
    float amount;
    float price;
    float fee;
    String yourAction;
    float bonus;
    long timestamp;
    String comment;
}
