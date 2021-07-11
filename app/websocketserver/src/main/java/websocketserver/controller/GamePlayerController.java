package websocketserver.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import websocketserver.service.GamePlayerService;
import websocketserver.viewmodel.GamePlayerViewModel;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/players")
public class GamePlayerController {

    @NonNull
    private final GamePlayerService gamePlayerService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserRegisterResponse registerPlayer(
            WebSession webSession,
            @RequestBody UserRegisterRequest userRegisterRequest
    ) {
        final var createdPlayer = gamePlayerService.createPlayer(
                userRegisterRequest.getPlayerName(),
                userRegisterRequest.getPlayerPassword()
        );

        webSession.save();
        return new UserRegisterResponse(new GamePlayerViewModel(
                createdPlayer.getPlayerId(),
                createdPlayer.getPlayerDisplayName(),
                createdPlayer.getIconUrl()));
    }

    @GetMapping("/mypage")
    public String mypage(Model model) {
        return "players/mypage";
    }

    @PostMapping("/")
    public GetPlayersResponse getPlayers(
            @RequestBody GetPlayersRequest getPlayersRequest
    ) {
        final var gamePlayers = new ArrayList<GamePlayerViewModel>();
        gamePlayerService.getPlayers(getPlayersRequest.getPlayerIds())
                .forEach(gamePlayer -> gamePlayers.add(new GamePlayerViewModel(
                        gamePlayer.getPlayerId(),
                        gamePlayer.getPlayerDisplayName(),
                        gamePlayer.getIconUrl())));

        return new GetPlayersResponse(gamePlayers);
    }

    @Value
    @Validated
    public static class UserRegisterRequest {
        @NotNull String playerName;
        @NotNull String playerPassword;
    }

    @Value
    @Validated
    public static class UserRegisterResponse {
        @NotNull GamePlayerViewModel gamePlayer;
    }

    @Value
    public static class GetPlayersRequest {
        List<String> playerIds;
    }

    @Value
    public static class GetPlayersResponse {
        List<GamePlayerViewModel> gamePlayers;
    }

}
