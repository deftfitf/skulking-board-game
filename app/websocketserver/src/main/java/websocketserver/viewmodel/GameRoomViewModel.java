package websocketserver.viewmodel;

import lombok.Value;

import java.util.List;

@Value
public class GameRoomViewModel {
    String gameRoomId;
    String roomOwnerId;
    String roomOwnerDisplayName;
    String roomOwnerIconUrl;
    String gameState;
    List<GamePlayerViewModel> joinedPlayers;
}