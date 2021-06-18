package websocketserver.service;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import websocketserver.repository.GamePlayerRepository;

@RequiredArgsConstructor
public class GamePlayerReactiveUserDetailsService implements ReactiveUserDetailsService {

    @NonNull
    private final GamePlayerRepository gamePlayerRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        return Mono.fromSupplier(() -> gamePlayerRepository.findById(username))
                .map(gamePlayerOpt -> (UserDetails) gamePlayerOpt
                        // TODO: error handling, when not found user, return status code 401
                        .orElseThrow(() -> new IllegalArgumentException("The specified user was not found")))
                .subscribeOn(Schedulers.single());
    }

}
