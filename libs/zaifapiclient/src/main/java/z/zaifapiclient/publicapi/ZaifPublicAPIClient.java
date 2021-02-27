package z.zaifapiclient.publicapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import z.zaifapiclient.publicapi.dto.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class ZaifPublicAPIClient {

    private ZaifPublicService zaifPublicService;

    public ZaifPublicAPIClient(String baseUrl) {
        final var objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);

        zaifPublicService = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(JacksonConverterFactory.create(objectMapper))
                .build()
                .create(ZaifPublicService.class);
    }

    public static ZaifPublicAPIClient createDefault() {
        final var defaultBaseUrl = "https://api.zaif.jp/api/1/";
        return new ZaifPublicAPIClient(defaultBaseUrl);
    }

    public CompletableFuture<List<Currency>> getAllCurrency() {
        return RetrofitHelper.toFuture(zaifPublicService.getAllCurrency());
    }

    public CompletableFuture<List<Currency>> getCurrency(String currency) {
        return RetrofitHelper.toFuture(zaifPublicService.getCurrency(currency));
    }

    public CompletableFuture<List<CurrencyPair>> getAllCurrencyPairs() {
        return RetrofitHelper.toFuture(zaifPublicService.getAllCurrencyPairs());
    }

    public CompletableFuture<List<CurrencyPair>> getCurrencyPair(String currencyPair) {
        return RetrofitHelper.toFuture(zaifPublicService.getCurrencyPair(currencyPair));
    }

    public CompletableFuture<LastPrice> getLastPrice(String currencyPair) {
        return RetrofitHelper.toFuture(zaifPublicService.getLastPrice(currencyPair));
    }

    public CompletableFuture<Ticker> getTicker(String currencyPair) {
        return RetrofitHelper.toFuture(zaifPublicService.getTicker(currencyPair));
    }

    public CompletableFuture<List<Trade>> getTrades(String currencyPair) {
        return RetrofitHelper.toFuture(zaifPublicService.getTrades(currencyPair));
    }

    public CompletableFuture<OrderBook> getOrderBook(String currencyPair) {
        return RetrofitHelper.toFuture(zaifPublicService.getOrderBook(currencyPair));
    }
}
