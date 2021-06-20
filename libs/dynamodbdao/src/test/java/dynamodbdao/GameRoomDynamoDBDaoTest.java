package dynamodbdao;

import dynamodbdao.beans.GameRoom;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.WriteBatch;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class GameRoomDynamoDBDaoTest {

    private static final String tableName = "game_room_" + UUID.randomUUID();
    private GameRoomDynamoDBDao dao;

    @Before
    public void setUp() {
        dao = new GameRoomDynamoDBDao(
                "http://localhost:8000",
                "fakeMyKeyId",
                "fakeSecretAccessKey",
                tableName
        );

        dao.baseClient.createTable(bldr -> bldr
                .tableName(tableName)
                .attributeDefinitions(List.of(
                        AttributeDefinition.builder()
                                .attributeName("gameRoomId")
                                .attributeType(ScalarAttributeType.S)
                                .build()
                ))
                .keySchema(List.of(
                        KeySchemaElement.builder()
                                .keyType(KeyType.HASH)
                                .attributeName("gameRoomId")
                                .build()
                ))
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build()
                ));
    }

    @After
    public void cleanUp() {
        dao.baseClient.deleteTable(bldr -> bldr.tableName(tableName));
    }

    @Test
    public void putNewRoom() {
        final var newRoom = new GameRoom();
        newRoom.setGameRoomId("gameroom1");
        newRoom.setRoomOwnerId("roomowner1");
        newRoom.setGameState("INITIALIZED");
        newRoom.setJoinedPlayerIds(List.of(newRoom.getRoomOwnerId()));

        dao.putNewRoom(newRoom);
        assertThat(dao.findById(newRoom.getGameRoomId())).isEqualTo(newRoom);

        assertThatThrownBy(() -> dao.putNewRoom(newRoom));
    }

    @Test
    public void updateRoom() {
        final var newRoom = new GameRoom();
        newRoom.setGameRoomId("gameroom1");
        newRoom.setRoomOwnerId("roomowner1");
        newRoom.setGameState("INITIALIZED");
        newRoom.setJoinedPlayerIds(List.of(newRoom.getRoomOwnerId()));

        dao.putNewRoom(newRoom);
        assertThat(dao.findById(newRoom.getGameRoomId())).isEqualTo(newRoom);

        final var room2 = new GameRoom();
        room2.setGameRoomId("gameroom1");
        room2.setRoomOwnerId("roomowner2");
        room2.setGameState("PLAYING");
        room2.setJoinedPlayerIds(List.of(newRoom.getGameRoomId(), room2.getGameRoomId()));

        dao.updateRoom(room2);
        assertThat(dao.findById(newRoom.getGameRoomId())).isEqualTo(room2);
    }

    @Test
    public void select() {
        final var rooms = java.util.stream.Stream.iterate(0, i -> i + 1)
                .map(i -> {
                    final var room = new GameRoom();
                    room.setGameRoomId(String.format("%03d", i));
                    room.setRoomOwnerId("roomowner" + i);
                    room.setGameState("INITIALIZED");
                    room.setJoinedPlayerIds(List.of(room.getRoomOwnerId()));
                    return room;
                })
                .limit(70)
                .collect(Collectors.toList());

        final var writeBatches = rooms.stream().map(room -> WriteBatch
                .builder(GameRoom.class)
                .mappedTableResource(dao.client
                        .table(tableName, TableSchema.fromClass(GameRoom.class)))
                .addPutItem(room)
                .build())
                .collect(Collectors.toList());

        Stream.iterate(0, i -> i < 70, i -> i + 25)
                .forEach(i -> dao.client.batchWriteItem(bldr -> bldr
                        .writeBatches(writeBatches.subList(i, Math.min(i + 25, 70)))));

        final var firstPage = dao.select(30, null);
        assertThat(firstPage.size()).isEqualTo(30);
        final var firstPageLastRoom = firstPage.get(firstPage.size() - 1);

        final var secondPage = dao.select(30, firstPageLastRoom.getGameRoomId());
        assertThat(secondPage.size()).isEqualTo(30);
        final var secondPageLastRoom = secondPage.get(secondPage.size() - 1);

        final var lastPage = dao.select(30, secondPageLastRoom.getGameRoomId());
        assertThat(lastPage.size()).isEqualTo(10);

        final var result = new HashSet<>();
        result.addAll(firstPage);
        result.addAll(secondPage);
        result.addAll(lastPage);
        assertThat(result.size()).isEqualTo(70);
        assertThat(result).containsAll(rooms);
    }
}