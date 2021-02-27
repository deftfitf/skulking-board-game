package z.zaifapiclient.cashtxapi;

import retrofit2.http.Body;
import retrofit2.http.POST;
import z.zaifapiclient.cashtxapi.dto.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ZaifCashTxService {

    @POST("/tapi")
    CompletableFuture<Balance> getBalance(@Body GetBalanceRequest getBalanceRequest);

    @POST("/tapi")
    CompletableFuture<Balance> getBalanceLight(@Body GetBalanceRequestLight getBalanceRequestLight);

    @POST("/tapi")
    CompletableFuture<IdInfo> getIdInfo(@Body GetIdInfoRequest getIdInfoRequest);

    @POST("/tapi")
    CompletableFuture<Map<String, OrderHistory>> getOrderHistories(@Body GetOrderHistoriesRequest getOrderHistoriesRequest);

    @POST("/tapi")
    CompletableFuture<Map<String, UnfilledOrder>> getUnfilledOrders(@Body GetUnfilledOrderRequest getUnfilledOrderRequest);

    @POST("/tapi")
    CompletableFuture<TradeResponse> trade(@Body TradeRequest tradeRequest);

    @POST("/tapi")
    CompletableFuture<CancelTradingResponse> cancelTrading(@Body CancelTradingRequest cancelTradingRequest);
    
}
