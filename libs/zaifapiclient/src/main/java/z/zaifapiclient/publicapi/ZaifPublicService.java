package z.zaifapiclient.publicapi;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;
import z.zaifapiclient.publicapi.dto.*;

import java.util.List;

public interface ZaifPublicService {

    @GET("currencies/all")
    Call<List<Currency>> getAllCurrency();

    @GET("currencies/{currency}")
    Call<List<Currency>> getCurrency(@Path("currency") String currency);

    @GET("currency_pairs/all")
    Call<List<CurrencyPair>> getAllCurrencyPairs();

    @GET("currency_pairs/{currencyPair}")
    Call<List<CurrencyPair>> getCurrencyPair(@Path("currencyPair") String currencyPair);

    @GET("last_price/{currencyPair}")
    Call<LastPrice> getLastPrice(@Path("currencyPair") String currencyPair);

    @GET("ticker/{currencyPair}")
    Call<Ticker> getTicker(@Path("currencyPair") String currencyPair);

    @GET("trades/{currencyPair}")
    Call<List<Trade>> getTrades(@Path("currencyPair") String currencyPair);

    @GET("depth/{currencyPair}")
    Call<OrderBook> getOrderBook(@Path("currencyPair") String currencyPair);

}
