package websocketserver.controller;

import dynamodbdao.GameRoomDynamoDBDao;
import dynamodbdao.beans.GameRoom;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/gamerooms")
public class GameRoomController {

    @NonNull
    private final GameRoomDynamoDBDao gameRoomDynamoDBDao;

    @PostMapping("/")
    public List<GameRoom> getGameRooms(
            @RequestBody GetGameRoomsRequest request
    ) {
        return gameRoomDynamoDBDao.select(
                request.getLimit(),
                request.getExclusiveStartKey());
    }

    @GetMapping("/{gameRoomId}")
    public GameRoom getGameRoom(@PathVariable String gameRoomId) {
        return gameRoomDynamoDBDao.findById(gameRoomId);
    }

    @Value
    public static class GetGameRoomsRequest {
        int limit;
        String exclusiveStartKey;
    }
}
