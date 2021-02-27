package z.zaifapiclient.cashtxapi.dto;

import lombok.Data;

import java.util.Map;

@Data
public class Balance {
    Map<String, Double> funds;
    Map<String, Double> deposit;
    Rights rights;
    Long tradeCount;
    long openOrders;
    long serverTime;

    @Data
    public static class Rights {
        int info;
        int trade;
        int withdraw;
        int personalInfo;
        int idInfo;
    }
}
