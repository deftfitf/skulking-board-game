package websocketserver.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import websocketserver.model.GamePlayer;
import websocketserver.repository.GamePlayerRepository;
import websocketserver.viewmodel.GamePlayerViewModel;

@RestController
@RequiredArgsConstructor
public class IndexRestController {

    @NonNull
    private final GamePlayerRepository gamePlayerRepository;

    @PostMapping("/checkLogin")
    public GamePlayerViewModel checkLogin(@AuthenticationPrincipal GamePlayer gamePlayer) {
        return new GamePlayerViewModel(
                gamePlayer.getPlayerId(),
                gamePlayer.getPlayerDisplayName(),
                "https://material-ui.com/static/images/avatar/1.jpg"
        );
    }

}
