package websocketserver.repository;

import org.socialsignin.spring.data.dynamodb.repository.EnableScan;
import org.springframework.data.repository.CrudRepository;
import websocketserver.model.GamePlayer;

@EnableScan
public interface GamePlayerRepository extends CrudRepository<GamePlayer, String> {
}
