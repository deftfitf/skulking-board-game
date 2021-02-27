package z.zaifapiclient.cashtxapi.dto;

import lombok.Data;

@Data
public class UnfilledOrder {
    int id;
    String currencyPair;
    String action;
    int price;
    long timestamp;
    String comment;
}
