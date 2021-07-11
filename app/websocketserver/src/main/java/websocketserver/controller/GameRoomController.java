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
import websocketserver.service.GamePlayerService;
import websocketserver.viewmodel.GamePlayerViewModel;
import websocketserver.viewmodel.GameRoomViewModel;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gamerooms")
public class GameRoomController {

    @NonNull
    private final GameServerServiceClient gameServerServiceClient;
    @NonNull
    private final GameRoomDynamoDBDao gameRoomDynamoDBDao;
    @NonNull
    private final GamePlayerService gamePlayerService;

    @PostMapping("/")
    public GetGameRoomsResponse getGameRooms(
            @RequestBody GetGameRoomsRequest request
    ) {
        final var dynamoGameRooms = gameRoomDynamoDBDao.select(
                request.getLimit(),
                request.getExclusiveStartKey());

        final var playerIds = dynamoGameRooms.stream()
                .flatMap(gameRoom ->
                        Stream.concat(
                                Stream.of(gameRoom.getRoomOwnerId()),
                                gameRoom.getJoinedPlayerIds().stream()))
                .distinct()
                .collect(Collectors.toList());

        final var playerIdToPlayer = new HashMap<String, GamePlayer>();
        gamePlayerService.getPlayers(playerIds).forEach(player -> {
            playerIdToPlayer.put(player.getPlayerId(), player);
        });

        final var gameRooms = dynamoGameRooms.stream().map(gameRoom -> {
            final var joinedPlayers = gameRoom.getJoinedPlayerIds().stream()
                    .map(playerId -> new GamePlayerViewModel(
                            playerId,
                            playerIdToPlayer.get(playerId).getPlayerDisplayName(),
                            playerIdToPlayer.get(playerId).getIconUrl()))
                    .collect(Collectors.toList());

            return new GameRoomViewModel(
                    gameRoom.getGameRoomId(),
                    gameRoom.getRoomOwnerId(),
                    playerIdToPlayer.get(gameRoom.getRoomOwnerId()).getPlayerDisplayName(),
                    playerIdToPlayer.get(gameRoom.getRoomOwnerId()).getIconUrl(),
                    gameRoom.getGameState(),
                    joinedPlayers);
        }).collect(Collectors.toList());

        return new GetGameRoomsResponse(gameRooms);
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

    @Value
    public static class GetGameRoomsResponse {
        List<GameRoomViewModel> gameRooms;
    }

}
