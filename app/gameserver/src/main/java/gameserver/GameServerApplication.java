package gameserver;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.http.javadsl.Http;
import akka.http.javadsl.ServerBinding;
import akka.management.cluster.bootstrap.ClusterBootstrap;
import akka.management.javadsl.AkkaManagement;
import gameserver.actor.GameRoomActor;
import gameserver.service.grpc.GameServerServiceHandlerFactory;
import gameserver.service.impl.*;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletionStage;

@Slf4j
public class GameServerApplication {

    public static void main(String[] args) {
        final ActorSystem<Void> system = ActorSystem.create(Behaviors.empty(), "GameServerSystem");
        try {
            init(system);
        } catch (Exception e) {
            log.error("Terminating due to initialization failure.", e);
            system.terminate();
        }
    }

    public static void init(ActorSystem<Void> system) {
        AkkaManagement.get(system).start();
        ClusterBootstrap.get(system).start();

        GameRoomActor.init(system);

        final var cardAdapter = new CardAdapter();
        final var gameRuleAdapter = new GameRuleAdapter();
        final var scoreBoardAdapter = new ScoreBoardAdapter();
        final var gameStateAdapter = new GameStateAdapter(gameRuleAdapter, cardAdapter, scoreBoardAdapter);
        final var gameEventAdapter = new GameEventAdapter(gameRuleAdapter, cardAdapter, scoreBoardAdapter, gameStateAdapter);
        final var gameCommandAdapter = new GameCommandAdapter(cardAdapter);
        final var service =
                GameServerServiceHandlerFactory.create(
                        new GameRoomServiceImpl(system, gameCommandAdapter, gameRuleAdapter, gameEventAdapter),
                        system);

        CompletionStage<ServerBinding> bound =
                Http.get(system)
                        .newServerAt("127.0.0.1", 8080)
                        .bind(service);

        bound.thenAccept(binding ->
                System.out.println("gRPC server bound to: " + binding.localAddress())
        );
    }

}
