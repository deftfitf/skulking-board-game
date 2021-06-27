package websocketserver.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import websocketserver.model.GamePlayer;
import websocketserver.repository.GamePlayerRepository;

@RestController
@RequiredArgsConstructor
public class IndexRestController {

    @NonNull
    private final GamePlayerRepository gamePlayerRepository;

    @PostMapping("/checkLogin")
    public GamePlayerResponse checkLogin(@AuthenticationPrincipal GamePlayer gamePlayer) {
        return new GamePlayerResponse(
                gamePlayer.getPlayerId(),
                gamePlayer.getPlayerDisplayName(),
                "https://material-ui.com/static/images/avatar/1.jpg"
        );
    }

    @Value
    public static class GamePlayerResponse {
        String playerId;
        String playerName;
        String icon;
    }

}
