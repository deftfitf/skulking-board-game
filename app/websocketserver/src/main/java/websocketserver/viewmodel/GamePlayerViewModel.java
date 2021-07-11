package websocketserver.viewmodel;

import lombok.Value;

@Value
public class GamePlayerViewModel {
    String playerId;
    String displayName;
    String iconUrl;
}