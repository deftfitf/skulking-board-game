package websocketserver.config;

import akka.actor.typed.ActorSystem;
import akka.actor.typed.javadsl.Behaviors;
import akka.grpc.GrpcClientSettings;
import gameserver.service.grpc.GameServerServiceClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GameServerClientConfig {

    @Bean(destroyMethod = "terminate")
    public ActorSystem<Void> actorSystem() {
        return ActorSystem.create(Behaviors.empty(), "GameServerClient");
    }

    @Bean
    public GameServerServiceClient gameServerServiceClient(
            ActorSystem<Void> system
    ) {
        return GameServerServiceClient.create(
                GrpcClientSettings
                        .fromConfig("game-server-system", system)
                        .withTls(false)
                        .withServicePortName("http"),
                system
        );
    }

}
