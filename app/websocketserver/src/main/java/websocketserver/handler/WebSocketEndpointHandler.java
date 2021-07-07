package websocketserver.handler;

import akka.actor.typed.ActorSystem;
import akka.stream.javadsl.Source;
import akka.stream.scaladsl.Sink;
import gameserver.service.grpc.GameCommand;
import gameserver.service.grpc.GameServerServiceClient;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.security.Principal;
import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEndpointHandler implements WebSocketHandler {

    @NonNull
    private final ActorSystem<Void> actorSystem;
    @NonNull
    private final GameServerServiceClient gameServerServiceClient;

    @Override
    public @NonNull Mono<Void> handle(WebSocketSession session) {
        final var playerIdMono = session
                .getHandshakeInfo().getPrincipal()
                .map(Principal::getName);

        final var gameCommandWithPrincipal = playerIdMono.flux()
                .flatMap(playerId -> session.receive()
                        .map(wsMessage -> {
                            try {
                                final var command = GameCommand
                                        .parseFrom(wsMessage.getPayload().asInputStream())
                                        .toBuilder()
                                        .setPlayerId(playerId)
                                        .build();
                                log.info("hoge: {}, {}", playerId, command);
                                return command;
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }));

        final var sessionSource = Source.fromPublisher(gameCommandWithPrincipal);

        final var retryableConnectionFlux = Flux
                .defer(() -> gameServerServiceClient
                        .connect(sessionSource)
                        .runWith(Sink.asPublisher(false), actorSystem))
                .doOnError(e -> log.error("There was an error connecting to the Grpc Game Server.", e))
                .retryWhen(Retry
                        .backoff(3, Duration.ofMillis(1000))
                        .jitter(0.5d)
                        .transientErrors(true))
                .map(event -> session.binaryMessage(factory -> factory.wrap(event.toByteArray())));

        return session.send(retryableConnectionFlux)
                .then()
                .doOnError(e -> log.error("inbound or outbound connection was closed", e))
                .doOnSuccess(notUsed -> log.info("successfully finished connection"));
    }

    public static void main(String[] args) throws InterruptedException {
        final var successFlux = Flux.interval(Duration.ofSeconds(2))
                .take(1)
                .doOnNext(l -> log.info("fallback checkpoint {}", l));
        final var failedFlux = Flux
                .interval(Duration.ofSeconds(3))
                .take(2)
                .doOnNext(l -> log.info("checkpoint 1, {}", l))
                .doOnError(e -> log.info("error 1"))
                .map(l -> l + 1)
                .doOnNext(l -> log.info("checkpoint 2, {}", l))
                .doOnError(e -> log.info("error 2"))
                .concatWith(Flux.error(new RuntimeException("error 1")))
                .retryWhen(Retry.max(1)
                        .doAfterRetry(s -> log.info("retry after"))
                        .doBeforeRetry(s -> log.info("retry before")))
                .onErrorResume(e -> {
                    log.error("error 1");
                    return successFlux;
                })
                .doOnError(e -> log.info("error 4"));

        failedFlux
                .doOnComplete(() -> log.info("complete !"))
                .subscribe();
        Thread.sleep(15000);
    }

}
