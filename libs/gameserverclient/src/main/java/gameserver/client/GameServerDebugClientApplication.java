package gameserver.client;

import akka.NotUsed;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.grpc.GrpcClientSettings;
import akka.stream.javadsl.Sink;
import akka.stream.javadsl.Source;
import gameserver.service.grpc.GameCommand;
import gameserver.service.grpc.GameRule;
import gameserver.service.grpc.GameServerServiceClient;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.UUID;

@Slf4j
public class GameServerDebugClientApplication {

    public static void main(String[] args) {
        final ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "GreeterClient");

        final var client = GameServerServiceClient.create(
                GrpcClientSettings.fromConfig("game-server-system", system)
                        .withTls(false)
                        .withServicePortName("http"),
                system
        );

        final var gameRoomId = UUID.randomUUID().toString();
        final var playerId = UUID.randomUUID().toString();

        log.info("gameRoomId: {}, playerId: {}", gameRoomId, playerId);
        final var gameRule = GameRule.newBuilder()
                .setRoomSize(5)
                .setNOfRounds(3)
                .setDeckType(GameRule.DeckType.STANDARD)
                .build();
        final Source<GameCommand, NotUsed> source = Source.from(List.of(
                GameCommand.newBuilder()
                        .setGameRoomId(gameRoomId)
                        .setPlayerId(playerId)
                        .setCreateRoom(GameCommand.CreateRoom.newBuilder()
                                .setGameRule(gameRule)
                                .build())
                        .build()
        ));

        final var responseSource = client.connect(source);

        responseSource
                .to(Sink.foreach(e -> log.info("event received: {}", e)))
                .run(system);
    }

}
