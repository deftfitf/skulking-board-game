package z.zaifapiclient.publicapi;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ZaifPublicAPIClientTest {

    final ZaifPublicAPIClient client = ZaifPublicAPIClient.createDefault();

    @Test
    public void getAllCurrency() {
        assertThat(client.getAllCurrency().join()).isNotEmpty();
    }

    @Test
    public void getCurrency() {
        assertThat(client.getCurrency("btc").join()).isNotNull();
    }

    @Test
    public void getAllCurrencyPair() {
        assertThat(client.getAllCurrencyPairs().join())
                .isNotEmpty();
    }

    @Test
    public void getCurrencyPair() {
        assertThat(client.getCurrencyPair("btc_jpy").join())
                .isNotEmpty();
    }

    @Test
    public void getLastPrice() {
        assertThat(client.getLastPrice("btc_jpy").join())
                .isNotNull();
    }

    @Test
    public void getTicker() {
        assertThat(client.getTicker("btc_jpy").join())
                .isNotNull();
    }

    @Test
    public void getTrades() {
        assertThat(client.getTrades("btc_jpy").join())
                .isNotEmpty();
    }

    @Test
    public void getOrderBook() {
        assertThat(client.getOrderBook("btc_jpy").join())
                .satisfies(orderBook -> {
                    assertThat(orderBook.getAsks().isEmpty()).isFalse();
                    assertThat(orderBook.getBids().isEmpty()).isFalse();
                });
    }
}