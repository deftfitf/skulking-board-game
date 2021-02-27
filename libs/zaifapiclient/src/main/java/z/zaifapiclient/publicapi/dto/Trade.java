package z.zaifapiclient.publicapi.dto;

import lombok.Data;

@Data
public class Trade {
    long date;
    float price;
    float amount;
    int tid;
    String currencyPair;
    String tradeType;
}
