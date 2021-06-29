package gameserver.service.impl;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.EntityRef;
import akka.stream.OverflowStrategy;
import akka.stream.javadsl.Keep;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import akka.stream.typed.javadsl.ActorSource;
import gameserver.actor.GameRoomActor;
import gameserver.domain.GameCommand;
import gameserver.domain.GameEvent;
import gameserver.domain.PlayerId;
import gameserver.service.grpc.GameServerService;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

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
    public Source<gameserver.service.grpc.GameEvent, NotUsed> connect(Source<gameserver.service.grpc.GameCommand, NotUsed> in) {
        log.info("New Connection Found");
        final var sharding = ClusterSharding.get(system);

        final var connectionSourceAndRoomRef = in.take(1)
                .initialTimeout(INITIAL_CONNECTION_TIMEOUT)
                .map(cmd -> {
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
                        case CREATE_ROOM:
                            final var gameRule = gameRuleAdapter.adapt(cmd.getCreateRoom().getGameRule());
                            connectionCommand = GameCommand.Init.builder()
                                    .gameRule(gameRule)
                                    .firstDealerId(playerId)
                                    .firstDealerRef(connectionRef)
                                    .build();
                            break;

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

                    return new ConnectionEstablished(playerId, actorSource, gameRoomActorRef);
                })
                .toMat(Sink.head(), Keep.right())
                .run(system)
                .toCompletableFuture()
                .join();

        final var connectionPlayerId = connectionSourceAndRoomRef.getConnectionPlayerId();
        final var connectionSource = connectionSourceAndRoomRef.getConnectionSource();
        final var roomRef = connectionSourceAndRoomRef.getGameCommandEntityRef();

        in.drop(1).to(Sink.foreach(_gameCommand -> {
            log.info("raw command: {}", _gameCommand);
            final var gameCommand = gameCommandAdapter.adapt(_gameCommand);
            roomRef.tell(gameCommand);
        })).run(system);

        return connectionSource
                .map(event -> gameEventAdapter.adapt(connectionPlayerId, event))
                .filter(Objects::nonNull)
                .keepAlive(KEEP_ALIVE_MESSAGE_DURATION, this::keepAliveEventSupplier);
    }

    @Value
    public static class ConnectionEstablished {
        PlayerId connectionPlayerId;
        Source<GameEvent, NotUsed> connectionSource;
        EntityRef<GameCommand> gameCommandEntityRef;
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
