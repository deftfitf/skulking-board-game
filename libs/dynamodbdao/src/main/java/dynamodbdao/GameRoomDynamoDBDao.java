package dynamodbdao;

import dynamodbdao.beans.GameRoom;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class GameRoomDynamoDBDao {

    protected final DynamoDbClient baseClient;
    protected final DynamoDbEnhancedClient client;
    protected final String tableName;

    public GameRoomDynamoDBDao(
            String endpoint, String accessKeyId, String accessKeySecret, String tableName
    ) {
        baseClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(endpoint))
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKeyId, accessKeySecret)))
                .build();
        client = DynamoDbEnhancedClient.builder()
                .dynamoDbClient(baseClient)
                .build();
        this.tableName = tableName;
    }

    private <T> DynamoDbTable<T> getTableFor(Class<T> clazz) {
        return client.table(tableName, TableSchema.fromBean(clazz));
    }

    public void putNewRoom(GameRoom gameRoom) {
        baseClient.putItem(PutItemRequest.builder()
                .tableName(tableName)
                .item(Map.of(
                        "gameRoomId", AttributeValue.builder().s(gameRoom.getGameRoomId()).build(),
                        "roomOwnerId", AttributeValue.builder().s(gameRoom.getRoomOwnerId()).build(),
                        "gameState", AttributeValue.builder().s(gameRoom.getGameState()).build(),
                        "joinedPlayerIds", AttributeValue.builder()
                                .l(gameRoom.getJoinedPlayerIds().stream()
                                        .map(joinedPlayerId -> AttributeValue.builder().s(joinedPlayerId).build())
                                        .collect(Collectors.toList()))
                                .build()))
                .conditionExpression("attribute_not_exists(gameRoomId)")
                .build());
    }

    public void updateRoom(GameRoom gameRoom) {
        final var table = getTableFor(GameRoom.class);
        table.putItem(gameRoom);
    }

    public GameRoom findById(String gameRoomId) {
        final var table = getTableFor(GameRoom.class);
        return table.getItem(Key.builder().partitionValue(gameRoomId).build());
    }

    public List<GameRoom> select(int limit, String exclusiveStartKey) {
        final var table = getTableFor(GameRoom.class);

        return table.scan(bldr -> bldr
                .limit(limit)
                .exclusiveStartKey(Optional
                        .ofNullable(exclusiveStartKey)
                        .map(k -> Map.of("gameRoomId", AttributeValue.builder().s(k).build()))
                        .orElse(null)))
                .items().stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    public GameRoom deleteRoom(String gameRoomId) {
        final var table = getTableFor(GameRoom.class);
        return table.deleteItem(Key.builder().partitionValue(gameRoomId).build());
    }

}
