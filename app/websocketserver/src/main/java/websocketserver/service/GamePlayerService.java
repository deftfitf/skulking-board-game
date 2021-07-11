package websocketserver.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import websocketserver.model.GamePlayer;
import websocketserver.repository.GamePlayerRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GamePlayerService {

    @NonNull
    private final GamePlayerRepository gamePlayerRepository;
    @NonNull
    private final PasswordEncoder passwordEncoder;

    public GamePlayer createPlayer(
            String playerName,
            String playerPassword
    ) {
        final var encodedPassword = passwordEncoder.encode(playerPassword);
        final var newPlayer = new GamePlayer(playerName, encodedPassword);

        return gamePlayerRepository.save(newPlayer);
    }

    public Iterable<GamePlayer> getPlayers(List<String> playerIds) {
        return gamePlayerRepository.findAllById(playerIds);
    }

}
