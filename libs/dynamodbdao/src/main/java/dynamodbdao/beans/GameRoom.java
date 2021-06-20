package dynamodbdao.beans;

import lombok.Data;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.util.List;

@DynamoDbBean
@Data
public class GameRoom {
    String gameRoomId;
    String roomOwnerId;
    String gameState;
    List<String> joinedPlayerIds;

    @DynamoDbPartitionKey
    public String getGameRoomId() {
        return gameRoomId;
    }

}
