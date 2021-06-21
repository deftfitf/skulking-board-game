package websocketserver.controller;

import dynamodbdao.GameRoomDynamoDBDao;
import dynamodbdao.beans.GameRoom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.WebTestClient;
import websocketserver.WebSocketServerApplication;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.csrf;
import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.springSecurity;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = WebSocketServerApplication.class)
public class GameRoomControllerTest {

    @Autowired
    ApplicationContext context;

    @MockBean
    GameRoomDynamoDBDao dao;

    WebTestClient client;

    @Before
    public void setUp() {
        client = WebTestClient
                .bindToApplicationContext(context)
                .apply(springSecurity())
                .configureClient()
                .build();
    }

    @WithMockUser(roles = "PLAYER")
    @Test
    public void canGetRooms() {
        final var gameRoom = new GameRoom();
        gameRoom.setGameRoomId("1");

        when(dao.select(anyInt(), eq(null))).thenReturn(List.of(gameRoom));

        final var request = new GameRoomController.GetGameRoomsRequest(
                100,
                null
        );

        client.mutateWith(csrf())
                .post()
                .uri("/gamerooms/")
                .bodyValue(request)
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBodyList(GameRoom.class)
                .contains(gameRoom);
    }

    @WithMockUser(roles = "PLAYER")
    @Test
    public void canGetRoom() {
        final var gameRoom = new GameRoom();
        gameRoom.setGameRoomId("1");

        when(dao.findById(gameRoom.getGameRoomId())).thenReturn(gameRoom);

        client
                .get()
                .uri("/gamerooms/" + gameRoom.getGameRoomId())
                .exchange()
                .expectStatus().is2xxSuccessful()
                .expectBody(GameRoom.class).isEqualTo(gameRoom);
    }

}