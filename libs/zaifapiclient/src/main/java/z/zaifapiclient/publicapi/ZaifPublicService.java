package z.zaifapiclient.publicapi;

import retrofit2.http.GET;
import retrofit2.http.Path;
import z.zaifapiclient.publicapi.dto.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ZaifPublicService {

    @GET("currencies/all")
    CompletableFuture<List<Currency>> getAllCurrency();

    @GET("currencies/{currency}")
    CompletableFuture<List<Currency>> getCurrency(@Path("currency") String currency);

    @GET("currency_pairs/all")
    CompletableFuture<List<CurrencyPair>> getAllCurrencyPairs();

    @GET("currency_pairs/{currencyPair}")
    CompletableFuture<List<CurrencyPair>> getCurrencyPair(@Path("currencyPair") String currencyPair);

    @GET("last_price/{currencyPair}")
    CompletableFuture<LastPrice> getLastPrice(@Path("currencyPair") String currencyPair);

    @GET("ticker/{currencyPair}")
    CompletableFuture<Ticker> getTicker(@Path("currencyPair") String currencyPair);

    @GET("trades/{currencyPair}")
    CompletableFuture<List<Trade>> getTrades(@Path("currencyPair") String currencyPair);

    @GET("depth/{currencyPair}")
    CompletableFuture<OrderBook> getOrderBook(@Path("currencyPair") String currencyPair);

}
