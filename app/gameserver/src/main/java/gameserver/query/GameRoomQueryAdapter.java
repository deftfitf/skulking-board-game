package gameserver.query;

import dynamodbdao.beans.GameRoom;
import gameserver.domain.GameState;
import gameserver.domain.PlayerId;

import java.util.stream.Collectors;

public class GameRoomQueryAdapter {

    private GameRoomQueryAdapter() {
    }

    public static GameRoom adapt(String gameRoomId, GameState gameState) {
        final var gameRoom = new GameRoom();
        gameRoom.setGameRoomId(gameRoomId);
        gameRoom.setRoomOwnerId(gameState.getRoomOwnerId().getValue());
        gameRoom.setJoinedPlayerIds(
                gameState.getPlayerIds().stream()
                        .map(PlayerId::getValue)
                        .collect(Collectors.toList()));
        gameRoom.setGameState(gameState.getStateName().name());
        return gameRoom;
    }

}
