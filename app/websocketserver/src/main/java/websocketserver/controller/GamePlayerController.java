package websocketserver.controller;

import lombok.*;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.WebSession;
import websocketserver.service.GamePlayerService;

import javax.validation.constraints.NotNull;

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
        return new UserRegisterResponse(createdPlayer.getUsername());
    }

    @PostMapping("/login")
    public void login() {
    }

    @PostMapping("/logout")
    public void logout() {
    }

    @GetMapping("/mypage")
    public String mypage(Model model) {
        return "players/mypage";
    }

    @Data
    @Validated
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegisterRequest {
        @NotNull String playerName;
        @NotNull String playerPassword;
    }

    @Data
    @Validated
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserRegisterResponse {
        @NotNull String playerId;
    }

}
