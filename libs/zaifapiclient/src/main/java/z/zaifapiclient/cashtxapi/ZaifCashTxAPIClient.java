package z.zaifapiclient.cashtxapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import z.zaifapiclient.cashtxapi.dto.*;
import z.zaifapiclient.cashtxapi.retrofit.ZaifConverterFactory;
import z.zaifapiclient.cashtxapi.retrofit.ZaifRequestInterceptor;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class ZaifCashTxAPIClient {

    private final String apiKey;
    private final ZaifCashTxService zaifCashTxService;

    public ZaifCashTxAPIClient(String apiKey, String secretKey, String baseUrl) {
        this.apiKey = apiKey;
        final var objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        final var client = new OkHttpClient.Builder()
                .addInterceptor(new ZaifRequestInterceptor(apiKey, secretKey))
                .build();

        zaifCashTxService = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(ZaifConverterFactory.create(objectMapper))
                .client(client)
                .build()
                .create(ZaifCashTxService.class);
    }

    public static ZaifCashTxAPIClient create(String apiKey, String secretKey) {
        return new ZaifCashTxAPIClient(apiKey, secretKey, "https://api.zaif.jp/");
    }

    public CompletableFuture<Balance> getBalance() {
        return zaifCashTxService.getBalance(GetBalanceRequest.builder().build());
    }

    public CompletableFuture<Balance> getBalanceLight() {
        return zaifCashTxService.getBalanceLight(GetBalanceRequestLight.builder().build());
    }

    public CompletableFuture<IdInfo> getIdInfo() {
        return zaifCashTxService.getIdInfo(GetIdInfoRequest.builder().build());
    }

    public CompletableFuture<Map<String, OrderHistory>> getOrderHistories(GetOrderHistoriesRequest getOrderHistoriesRequest) {
        return zaifCashTxService.getOrderHistories(getOrderHistoriesRequest);
    }

    public CompletableFuture<Map<String, UnfilledOrder>> getUnfilledOrders(GetUnfilledOrderRequest getUnfilledOrderRequest) {
        return zaifCashTxService.getUnfilledOrders(getUnfilledOrderRequest);
    }

    public CompletableFuture<TradeResponse> trade(TradeRequest tradeRequest) {
        return zaifCashTxService.trade(tradeRequest);
    }

    public CompletableFuture<CancelTradingResponse> cancelTrading(CancelTradingRequest cancelTradingRequest) {
        return zaifCashTxService.cancelTrading(cancelTradingRequest);
    }

}
