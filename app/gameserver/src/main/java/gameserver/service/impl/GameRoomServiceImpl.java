package gameserver.service.impl;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.typed.javadsl.ActorSource;
import gameserver.actor.GameRoomActor;
import gameserver.domain.GameCommand;
import gameserver.domain.GameEvent;
import gameserver.domain.PlayerId;
import gameserver.service.grpc.CreateRoom;
import gameserver.service.grpc.GameServerService;
import gameserver.service.grpc.Initialized;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletionStage;

@Slf4j
@RequiredArgsConstructor
public class GameRoomServiceImpl implements GameServerService {

    private static final int ACTOR_SOURCE_BUFFER_SIZE = 100;
    private static final Duration INITIAL_CONNECTION_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration KEEP_ALIVE_MESSAGE_DURATION = Duration.ofSeconds(5);

    private final ActorSystem<?> system;
    private final GameCommandAdapter gameCommandAdapter;
    private final GameRuleAdapter gameRuleAdapter;
    private final GameEventAdapter gameEventAdapter;

    @Override
    public CompletionStage<Initialized> create(CreateRoom in) {
        log.info("New Create Request: {}", in);
        final var sharding = ClusterSharding.get(system);

        final var gameRule = gameRuleAdapter.adapt(in.getGameRule());
        final var gameRoomId = UUID.randomUUID().toString();
        final var gameRoomActorRef =
                sharding.entityRefFor(GameRoomActor.ENTITY_TYPE_KEY, gameRoomId);

        final var playerId = new PlayerId(in.getPlayerId());
        return gameRoomActorRef.<GameEvent.Initialized>ask(res -> GameCommand.Init.builder()
                        .gameRule(gameRule)
                        .firstDealerId(playerId)
                        .response(res)
                        .build(),
                Duration.ofSeconds(10))
                .thenApply(gameEventAdapter::adapt);
    }

    @Override
    public Source<gameserver.service.grpc.GameEvent, NotUsed> connect(Source<gameserver.service.grpc.GameCommand, NotUsed> in) {
        log.info("New Connection Found");
        final var sharding = ClusterSharding.get(system);

        return in
                .initialTimeout(INITIAL_CONNECTION_TIMEOUT)
                .prefixAndTail(1)
                .flatMapConcat(headAndTail -> {
                    final var cmd = headAndTail.first().get(0);

                    final var playerId = new PlayerId(cmd.getPlayerId());
                    final var gameRoomId = cmd.getGameRoomId();
                    final var gameRoomActorRef =
                            sharding.entityRefFor(GameRoomActor.ENTITY_TYPE_KEY, gameRoomId);

                    final var connectionActorSource =
                            ActorSource.actorRef(
                                    this::actorSourceCompletionMatcher,
                                    gameEvent -> Optional.empty(),
                                    ACTOR_SOURCE_BUFFER_SIZE,
                                    OverflowStrategy.dropNew());
                    final var connectionRefAndSource = connectionActorSource.preMaterialize(system);
                    final var connectionRef = connectionRefAndSource.first();
                    final var actorSource = connectionRefAndSource.second();

                    final GameCommand connectionCommand;
                    switch (cmd.getCmdCase()) {
                        case JOIN:
                            connectionCommand = GameCommand.Join.builder()
                                    .playerId(playerId)
                                    .playerRef(connectionRef)
                                    .build();
                            break;

                        case NEW_CONNECTION:
                            connectionCommand = GameCommand.NewConnection.builder()
                                    .playerId(playerId).playerRef(connectionRef)
                                    .build();
                            break;

                        default:
                            throw new RuntimeException("illegal command detected. first is new connection only allowed");
                    }
                    gameRoomActorRef.tell(connectionCommand);

                    headAndTail.second().to(Sink.foreach(_gameCommand -> {
                        log.info("raw command: {}", _gameCommand);
                        final var gameCommand = gameCommandAdapter.adapt(connectionRef, _gameCommand);
                        gameRoomActorRef.tell(gameCommand);
                    })).run(system);

                    return actorSource
                            .map(event -> gameEventAdapter.adapt(playerId, event))
                            .filter(Objects::nonNull);
                })
                .keepAlive(KEEP_ALIVE_MESSAGE_DURATION, this::keepAliveEventSupplier);
    }

    private boolean actorSourceCompletionMatcher(GameEvent gameEvent) {
        return gameEvent instanceof GameEvent.GameEnded ||
                gameEvent instanceof GameEvent.ConnectionClosed;
    }

    private gameserver.service.grpc.GameEvent keepAliveEventSupplier() {
        return gameserver.service.grpc.GameEvent.newBuilder()
                .setKeepAlive(gameserver.service.grpc.GameEvent.KeepAlive.newBuilder().build())
                .build();
    }

}
