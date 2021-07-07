package websocketserver.controller;

import dynamodbdao.GameRoomDynamoDBDao;
import dynamodbdao.beans.GameRoom;
import gameserver.domain.GameRule;
import gameserver.service.grpc.CreateRoom;
import gameserver.service.grpc.GameServerServiceClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import websocketserver.model.GamePlayer;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gamerooms")
public class GameRoomController {

    @NonNull
    private final GameServerServiceClient gameServerServiceClient;
    @NonNull
    private final GameRoomDynamoDBDao gameRoomDynamoDBDao;

    @PostMapping("/")
    public List<GameRoom> getGameRooms(
            @RequestBody GetGameRoomsRequest request
    ) {
        return gameRoomDynamoDBDao.select(
                request.getLimit(),
                request.getExclusiveStartKey());
    }

    @PostMapping("/create")
    public String createGameRoom(
            @AuthenticationPrincipal GamePlayer gamePlayer,
            @RequestBody GameRule gameRule
    ) {
        // unify implementation to GameRuleAdapter
        final gameserver.service.grpc.GameRule.DeckType deckType;
        switch (gameRule.getDeckType()) {
            case STANDARD:
                deckType = gameserver.service.grpc.GameRule.DeckType.STANDARD;
                break;
            case EXPANSION:
                deckType = gameserver.service.grpc.GameRule.DeckType.EXPANSION;
                break;
            default:
                throw new IllegalArgumentException("illegal deck type specified");
        }

        final var res = gameServerServiceClient.create(CreateRoom.newBuilder()
                .setPlayerId(gamePlayer.getPlayerId())
                .setGameRule(gameserver.service.grpc.GameRule.newBuilder()
                        .setRoomSize(gameRule.getRoomSize())
                        .setNOfRounds(gameRule.getNOfRounds())
                        .setDeckType(deckType)
                        .build()
                )
                .build())
                .toCompletableFuture()
                .join();

        return res.getGameRoomId();
    }

    @GetMapping("/{gameRoomId}")
    public GameRoom getGameRoom(@PathVariable String gameRoomId) {
        return gameRoomDynamoDBDao.findById(gameRoomId);
    }

    @Value
    public static class GetGameRoomsRequest {
        int limit;
        String exclusiveStartKey;
    }
}
