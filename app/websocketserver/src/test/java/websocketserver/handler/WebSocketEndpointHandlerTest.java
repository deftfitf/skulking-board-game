package websocketserver.handler;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.web.reactive.function.BodyInserters;
import websocketserver.WebSocketServerApplication;
import websocketserver.controller.GamePlayerController;
import websocketserver.repository.GamePlayerRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = WebSocketServerApplication.class)
public class WebSocketEndpointHandlerTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    GamePlayerRepository repository;

    WebTestClient client;

    @Before
    public void setUp() {
        client = WebTestClient
                .bindToApplicationContext(context)
                // spring security のテストサポート機能を適用するために呼ぶ
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @After
    public void cleanup() {
        repository.deleteAll();
    }

    @Test
    public void canDisplayTopPage() {
        client.get()
                .uri("/")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    public void canRegisterNewPlayer() {
        final var request = new GamePlayerController.UserRegisterRequest(
                "player1",
                "password"
        );

        final var response = client.mutateWith(csrf())
                .post()
                .uri("/players/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(GamePlayerController.UserRegisterResponse.class)
                .getResponseBody()
                .blockFirst();

        assertThat(response.getPlayerId()).isEqualTo(response.getPlayerId());
    }

    @Test
    public void canLoginAndLogoutPlayer() {
        final var request = new GamePlayerController.UserRegisterRequest(
                "player1",
                "password"
        );

        final var response = client.mutateWith(csrf())
                .post()
                .uri("/players/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(GamePlayerController.UserRegisterResponse.class)
                .getResponseBody()
                .blockFirst();

        assertThat(response.getPlayerId()).isEqualTo(response.getPlayerId());

        final var loginSuccessful = client
                .post()
                .uri("/login")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(BodyInserters.fromFormData(new MultiValueMapAdapter<>(Map.of(
                        "username", List.of(response.getPlayerId()),
                        "password", List.of(request.getPlayerPassword())))))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/")
                .expectCookie().exists("SESSION")
                .returnResult(Void.class);

        final var session = loginSuccessful
                .getResponseCookies()
                .getFirst("SESSION");
        final var csrf = loginSuccessful
                .getResponseCookies()
                .getFirst("XSRF-TOKEN");

        final var getMyPage = client
                .get()
                .uri("/players/mypage")
                .cookies(cookies -> {
                    cookies.add("SESSION", session.getValue());
                    cookies.add("XSRF-TOKEN", csrf.getValue());
                })
                .exchange()
                .expectStatus().is2xxSuccessful()
                .returnResult(Void.class);

        client
                .post()
                .uri("/logout")
                .cookies(cookies -> cookies.add("SESSION", session.getValue()))
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueEquals("Location", "/");

        client.mutateWith(csrf())
                .get()
                .uri("/players/mypage")
                .cookies(cookies -> cookies.add("SESSION", session.getValue()))
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @WithMockUser(roles = "PLAYER")
    @Test
    public void canDisplayMyPage() {
        client.mutateWith(csrf())
                .get()
                .uri("/players/mypage")
                .exchange()
                .expectStatus().is2xxSuccessful();
    }

    @Test
    public void gameServerIsForbidden() throws Exception {
        client.post()
                .uri("/gameserver")
                .exchange()
                .expectStatus().isForbidden();
    }

}