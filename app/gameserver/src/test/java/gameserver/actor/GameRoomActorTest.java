package gameserver.actor;

import akka.actor.testkit.typed.javadsl.TestKitJunitResource;
import akka.actor.testkit.typed.javadsl.TestProbe;
import akka.actor.typed.ActorRef;
import dynamodbdao.GameRoomDynamoDBDao;
import gameserver.domain.*;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class GameRoomActorTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    @Mock
    private GameRoomDynamoDBDao dao;

    @ClassRule
    public static final TestKitJunitResource testKit = new TestKitJunitResource(
            "akka.persistence.journal.plugin = \"akka.persistence.journal.inmem\" \n" +
                    "akka.persistence.snapshot-store.plugin = \"akka.persistence.snapshot-store.local\"  \n" +
                    "akka.persistence.snapshot-store.local.dir = \"target/snapshot-" + UUID.randomUUID().toString() + "\"  \n"
    );

    private static AtomicInteger counter = new AtomicInteger();

    private static String newGameRoomId() {
        return "gameRoom-" + counter.incrementAndGet();
    }

    ///////////////////////////// NULL STATE /////////////////////////////

    @Test
    public void initialize() {
        final var gameRoomId = newGameRoomId();
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(gameRoomId, dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);

        final var init = GameCommand.Init.builder()
                .gameRule(gameRule).firstDealerId(dealer).response(probe.getRef().narrow()).build();
        gameRoom.tell(init);

        final var initialized = probe.receiveMessage();
        assertThat(initialized)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.Initialized.class))
                .satisfies(e -> {
                    assertThat(e.getGameRoomId()).isEqualTo(gameRoomId);
                    assertThat(e.getFirstDealerId()).isEqualTo(dealer);
                    assertThat(e.getGameRule()).isEqualTo(gameRule);
                });

        verify(dao, times(1)).putNewRoom(any());
    }

    ///////////////////////////// START PHASE /////////////////////////////

    @Test
    public void tooManyParticipantsAndAlreadyJoinedOnStartPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> dealerProbe = testKit.createTestProbe();
        final TestProbe<GameEvent> participantProbe = testKit.createTestProbe();
        final TestProbe<GameEvent> participant2Probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var gameRule = new GameRule(2, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<PlayerId>();
        playerIds.add(dealer);
        final var state = GameState.StartPhase.builder().dealerId(dealer).playerIds(playerIds).rule(gameRule).build();

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(dealerProbe.getRef()).build());
        final var connectionEstablished = dealerProbe.receiveMessage();
        assertThat(connectionEstablished)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.ConnectionEstablished.class))
                .satisfies(e -> assertThat(e.getPlayerId()).isEqualTo(dealer));
        dealerProbe.expectMessageClass(GameEvent.GameSnapshot.class);

        final var participant = new PlayerId("participant");
        final var participantJoin = GameCommand.Join.builder().playerId(participant).playerRef(participantProbe.getRef()).build();
        gameRoom.tell(participantJoin);
        participantProbe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        participantProbe.expectMessageClass(GameEvent.GameSnapshot.class);
        final var aPlayerJoined = participantProbe.receiveMessage();
        assertThat(aPlayerJoined)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.APlayerJoined.class))
                .satisfies(e -> assertThat(e.getPlayerId()).isEqualTo(participant));

        // The same message is being broadcast.
        assertThat(dealerProbe.receiveMessage()).isEqualTo(aPlayerJoined);

        final var participant2 = new PlayerId("participant2");
        gameRoom.tell(GameCommand.Join.builder().playerId(participant2).playerRef(participant2Probe.getRef()).build());
        final var aPlayerJoined2 = participant2Probe.receiveMessage();
        assertThat(aPlayerJoined2)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> assertThat(e.getInvalidInputType()).isEqualTo(InputCheckResult.InvalidInputType.FAILED_JOIN_EXCEED_MAX_NUMBER_OF_PLAYERS));

        gameRoom.tell(participantJoin);
        final var alreadyJoined = participantProbe.receiveMessage();
        assertThat(alreadyJoined)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> assertThat(e.getInvalidInputType()).isEqualTo(InputCheckResult.InvalidInputType.FAILED_JOIN_ALREADY_JOINED_PLAYER));
    }

    @Test
    public void cantLeavePlayerNotExistsOnStartPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<PlayerId>();
        playerIds.add(dealer);
        final var state = GameState.StartPhase.builder().dealerId(dealer).playerIds(playerIds).rule(gameRule).build();

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        gameRoom.tell(GameCommand.Leave.builder().playerId(new PlayerId("notExistsPlayerId")).playerRef(probe.getRef()).build());
        final var playerNotExists = probe.receiveMessage();
        assertThat(playerNotExists)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> assertThat(e.getInvalidInputType()).isEqualTo(InputCheckResult.InvalidInputType.FAILED_LEAVE_SPECIFIED_PLAYER_NOT_EXISTS));
    }

    @Test
    public void playerCanParticipantAndLeaveOnStartPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();
        final TestProbe<GameEvent> participantProbe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<PlayerId>();
        playerIds.add(dealer);
        final var state = GameState.StartPhase.builder().dealerId(dealer).playerIds(playerIds).rule(gameRule).build();

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        final var participant = new PlayerId("participant");
        final var participantJoin = GameCommand.Join.builder().playerId(participant).playerRef(participantProbe.getRef()).build();
        gameRoom.tell(participantJoin);
        participantProbe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        participantProbe.expectMessageClass(GameEvent.GameSnapshot.class);
        final var aPlayerJoined = participantProbe.receiveMessage();
        assertThat(aPlayerJoined)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.APlayerJoined.class))
                .satisfies(e -> assertThat(e.getPlayerId()).isEqualTo(participant));

        gameRoom.tell(GameCommand.Leave.builder().playerId(participant).playerRef(participantProbe.getRef()).build());
        final var participantLeft = participantProbe.receiveMessage();
        assertThat(participantLeft)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.APlayerLeft.class))
                .satisfies(e -> assertThat(e.getPlayerId()).isEqualTo(participant));

        participantProbe.expectMessage(GameEvent.ConnectionClosed.builder().playerId(participant).build());

        verify(dao, times(2)).updateRoom(any());
    }

    @Test
    public void changeDealerOnStartPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");
        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var state = GameState.StartPhase.builder().dealerId(dealer).playerIds(playerIds).rule(gameRule).build();

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        gameRoom.tell(GameCommand.Leave.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessage(GameEvent.RoomDealerChanged.builder()
                .newDealer(participant).oldDealer(dealer).build());

        probe.expectMessage(GameEvent.ConnectionClosed.builder()
                .playerId(dealer).build());

        gameRoom.tell(GameCommand.NewConnection.builder().playerId(participant).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);
        gameRoom.tell(GameCommand.SnapshotRequest.builder().playerId(participant).build());

        assertThat(probe.receiveMessage())
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameSnapshot.class))
                .satisfies(e -> {
                    final var s = (GameState.StartPhase) e.getGameState();
                    assertThat(s.getDealerId()).isEqualTo(participant);
                    assertThat(s.getPlayerIds().size()).isEqualTo(1);
                    assertThat(s.getPlayerIds().get(0)).isEqualTo(participant);
                });
    }

    @Test
    public void notEnoughPeopleAtTheStartOnStartPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<PlayerId>();
        playerIds.add(dealer);
        final var state = GameState.StartPhase.builder().dealerId(dealer).playerIds(playerIds).rule(gameRule).build();

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        gameRoom.tell(GameCommand.GameStart.builder().playerId(dealer).build());
        final var gameException = probe.receiveMessage();
        assertThat(gameException)
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> {
                    assertThat(e.getInvalidInputType())
                            .isEqualTo(InputCheckResult.InvalidInputType.FAILED_START_INSUFFICIENT_PLAYERS);
                });
    }

    @Test
    public void canStartOnStartPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();
        final TestProbe<GameEvent> participantProbe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant1 = new PlayerId("participant1");
        final var participant2 = new PlayerId("participant2");
        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<>(List.of(dealer, participant1, participant2));
        final var state = GameState.StartPhase.builder().dealerId(dealer).playerIds(playerIds).rule(gameRule).build();

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(participant1).playerRef(participantProbe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);
        participantProbe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        participantProbe.expectMessageClass(GameEvent.GameSnapshot.class);

        gameRoom.tell(GameCommand.GameStart.builder().playerId(participant1).build());
        assertThat(participantProbe.receiveMessage())
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> {
                    assertThat(e.getInvalidInputType())
                            .isEqualTo(InputCheckResult.InvalidInputType.FAILED_START_GAME_NOT_DEALER);
                });

        gameRoom.tell(GameCommand.GameStart.builder().playerId(dealer).build());
        probe.expectMessageClass(GameEvent.GameStarted.class);
        participantProbe.expectMessageClass(GameEvent.GameStarted.class);

        final var roundStarted = probe.receiveMessage();
        assertThat(roundStarted).isEqualTo(participantProbe.receiveMessage());
        assertThat(roundStarted).asInstanceOf(InstanceOfAssertFactories.type(GameEvent.RoundStarted.class))
                .satisfies(e -> {
                    final var joinedPlayer = e.getPlayers();
                    assertThat(joinedPlayer.size()).isEqualTo(3);
                    joinedPlayer.forEach(p -> assertThat(p.getCardIds().size()).isEqualTo(1));
                });

        probe.expectMessageClass(GameEvent.BiddingStarted.class);
        participantProbe.expectMessageClass(GameEvent.BiddingStarted.class);
    }

    ///////////////////////////// BIDDING PHASE /////////////////////////////

    @Test
    public void canBidDeclareAndStartTrickPhaseOnBiddingPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();
        final TestProbe<GameEvent> participantProbe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var state = GameState.BiddingPhase.newGame(dealer, gameRule, dealer, playerIds);

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(participant).playerRef(participantProbe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);
        participantProbe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        participantProbe.expectMessageClass(GameEvent.GameSnapshot.class);

        // declare bid
        gameRoom.tell(GameCommand.BidDeclare.builder().playerId(dealer).bid(0).build());
        final var dealerBidDeclared = GameEvent.APlayerBidDeclared.builder()
                .playerId(dealer).bidDeclared(0).build();
        probe.expectMessage(dealerBidDeclared);
        participantProbe.expectMessage(dealerBidDeclared);

        probe.expectNoMessage();
        participantProbe.expectNoMessage();

        // change bid declared
        gameRoom.tell(GameCommand.BidDeclare.builder().playerId(dealer).bid(1).build());
        final var dealerBidDeclareChanged = GameEvent.APlayerBidDeclared.builder()
                .playerId(dealer).bidDeclared(1).build();
        probe.expectMessage(dealerBidDeclareChanged);
        participantProbe.expectMessage(dealerBidDeclareChanged);

        // declare bid and start trick phase
        gameRoom.tell(GameCommand.BidDeclare.builder().playerId(participant).bid(0).build());
        final var participantBidDeclared = GameEvent.APlayerBidDeclared.builder()
                .playerId(participant).bidDeclared(0).build();
        probe.expectMessage(participantBidDeclared);
        participantProbe.expectMessage(participantBidDeclared);

        probe.expectMessageClass(GameEvent.TrickStarted.class);
        participantProbe.expectMessageClass(GameEvent.TrickStarted.class);
    }

    @Test
    public void bidDeclaredPlayerNotParticipantOrInvalidBidValueOnBiddingPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.STANDARD);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var state = GameState.BiddingPhase.newGame(dealer, gameRule, dealer, playerIds);

        gameRoom.tell(GameCommand.Store.builder().state(state).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        gameRoom.tell(GameCommand.BidDeclare.builder().playerId(new PlayerId("notParticipantId")).bid(3).build());
        probe.expectNoMessage();

        gameRoom.tell(GameCommand.BidDeclare.builder().playerId(dealer).bid(2).build());
        assertThat(probe.receiveMessage())
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> {
                    assertThat(e.getInvalidInputType())
                            .isEqualTo(InputCheckResult.InvalidInputType.DECLARED_INVALID_BID_VALUE);
                });

        gameRoom.tell(GameCommand.BidDeclare.builder().playerId(dealer).bid(-1).build());
        assertThat(probe.receiveMessage())
                .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameException.class))
                .satisfies(e -> {
                    assertThat(e.getInvalidInputType())
                            .isEqualTo(InputCheckResult.InvalidInputType.DECLARED_INVALID_BID_VALUE);
                });
    }

    ///////////////////////////// TRICK PHASE /////////////////////////////

    @Test
    public void canPlayCardOnTrickPhase() {
        Arrays.stream(GameRule.DeckType.values()).forEach(rule -> {
            final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
            final TestProbe<GameEvent> probe = testKit.createTestProbe();

            final var dealer = new PlayerId("dealer");
            final var participant = new PlayerId("participant");

            final var gameRule = new GameRule(5, 3, rule);
            final var playerIds = new ArrayList<>(List.of(dealer, participant));
            final var state = GameState.BiddingPhase.newGame(dealer, gameRule, dealer, playerIds);
            state.bid(dealer, 0);
            state.bid(participant, 1);
            final var trickState = state.startTrick();
            final var greenCard = new Card.NumberCard(new CardId("1"), 10, Card.NumberCard.CardColor.GREEN);
            final var purpleCard = new Card.NumberCard(new CardId("2"), 9, Card.NumberCard.CardColor.PURPLE);
            trickState.getPlayerOf(dealer).setCards(new HashMap<>(Map.of(greenCard.getCardId(), greenCard)));
            trickState.getPlayerOf(participant).setCards(new HashMap<>(Map.of(purpleCard.getCardId(), purpleCard)));

            gameRoom.tell(GameCommand.Store.builder().state(trickState).build());
            gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
            probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
            probe.expectMessageClass(GameEvent.GameSnapshot.class);

            // dealer plays trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(greenCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(dealer).playedCard(greenCard).build());

            // participant plays trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(purpleCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(participant).playedCard(purpleCard).build());

            // trick finished
            probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(dealer).card(greenCard).trickBonus(0).build());
            // round finished
            probe.expectMessage(GameEvent.RoundFinished.builder()
                    .roundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0))).build());
            // round started
            final var roundStarted = probe.receiveMessage();
            assertThat(roundStarted).asInstanceOf(InstanceOfAssertFactories.type(GameEvent.RoundStarted.class))
                    .satisfies(e -> {
                        final var joinedPlayer = e.getPlayers();
                        assertThat(joinedPlayer.size()).isEqualTo(2);
                        joinedPlayer.forEach(p -> assertThat(p.getCardIds().size()).isEqualTo(2)); // 2 cards are dealt
                        assertThat(e.getRound()).isEqualTo(2);
                    });
            // bidding phase started
            probe.expectMessageClass(GameEvent.BiddingStarted.class);

            gameRoom.tell(GameCommand.SnapshotRequest.builder().playerId(dealer).build());
            // new bidding phase
            assertThat(probe.receiveMessage())
                    .asInstanceOf(InstanceOfAssertFactories.type(GameEvent.GameSnapshot.class))
                    .satisfies(snapshot -> assertThat(snapshot.getGameState())
                            .asInstanceOf(InstanceOfAssertFactories.type(GameState.BiddingPhase.class))
                            .satisfies(biddingPhase -> {
                                assertThat(biddingPhase.getRound()).isEqualTo(2);
                                assertThat(biddingPhase.getDealerId()).isEqualTo(dealer);
                            }));
        });
    }

    @Test
    public void canPlayCardAndFinishGameOnTrickPhaseLastRound() {
        Arrays.stream(GameRule.DeckType.values()).forEach(rule -> {
            final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
            final TestProbe<GameEvent> probe = testKit.createTestProbe();

            final var dealer = new PlayerId("dealer");
            final var participant = new PlayerId("participant");

            final var gameRule = new GameRule(5, 3, rule);
            final var playerIds = new ArrayList<>(List.of(dealer, participant));
            final var scoreBoard = ScoreBoard.empty();
            scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
            scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
            final var state =
                    GameState.BiddingPhase.startRound(dealer, 3, gameRule, dealer, playerIds, scoreBoard);
            state.bid(dealer, 0);
            state.bid(participant, 2);
            final var trickState = state.startTrick();

            // for 1st trick
            final var greenCard = new Card.NumberCard(new CardId("1"), 10, Card.NumberCard.CardColor.GREEN);
            final var purpleCard = new Card.NumberCard(new CardId("2"), 9, Card.NumberCard.CardColor.PURPLE);

            // for second trick
            final var blackCard = new Card.NumberCard(new CardId("4"), 14, Card.NumberCard.CardColor.BLACK);
            final var pirateCard = new Card.Pirates.StandardPirates(new CardId("3"));

            // for third trick
            final var escapeCard = new Card.Escape.StandardEscape(new CardId("5"));
            final var tigressCard = new Card.Escape.Tigress(new CardId("6"), null);

            trickState.getPlayerOf(dealer).setCards(new HashMap<>(Map.of(
                    greenCard.getCardId(), greenCard, blackCard.getCardId(), blackCard, escapeCard.getCardId(), escapeCard)));
            trickState.getPlayerOf(participant).setCards(new HashMap<>(Map.of(
                    purpleCard.getCardId(), purpleCard, pirateCard.getCardId(), pirateCard, tigressCard.getCardId(), tigressCard)));

            gameRoom.tell(GameCommand.Store.builder().state(trickState).build());
            gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
            probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
            probe.expectMessageClass(GameEvent.GameSnapshot.class);

            // dealer plays 1st trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(greenCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(dealer).playedCard(greenCard).build());
            // participant plays 1st trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(purpleCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(participant).playedCard(purpleCard).build());
            // 1st trick finished
            probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(dealer).card(greenCard).trickBonus(0).build());

            // dealer plays 2nd trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(blackCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(dealer).playedCard(blackCard).build());
            // participant plays 2nd trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(pirateCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(participant).playedCard(pirateCard).build());
            // 2nd trick finished
            probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(pirateCard).trickBonus(20).build());

            // dealer plays 3rd trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(tigressCard.withIsPirates(false)).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(participant).playedCard(tigressCard.withIsPirates(false)).build());
            // participant plays 3rd trick
            gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(escapeCard).build());
            probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                    .playerId(dealer).playedCard(escapeCard).build());
            // 3rd trick finished
            probe.expectMessage(GameEvent.AllRanAway.builder().winnerId(participant).card(tigressCard.withIsPirates(false)).build());

            // round finished
            probe.expectMessage(GameEvent.RoundFinished.builder()
                    .roundScore(Map.of(dealer, new Score(-30, 0), participant, new Score(40, 20))).build());

            final var resultScoreBoard = ScoreBoard.empty();
            resultScoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
            resultScoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
            resultScoreBoard.addRoundScore(Map.of(dealer, new Score(-30, 0), participant, new Score(40, 20)));

            // game finished
            probe.expectMessage(GameEvent.GameFinished.builder()
                    .gameWinnerId(participant)
                    .scoreBoard(resultScoreBoard)
                    .build());

            assertThat(resultScoreBoard.getLastRoundScore())
                    .isEqualTo(scoreBoard.getLastRoundScore());
        });

        verify(dao, times(2)).updateRoom(any());
    }

    // TODO: add exceptional cases

    ///////////////////////////// NextLeadPlayerChanging PHASE /////////////////////////////

    @Test
    public void transitionFromTrickPhaseToNextTrickLeadPlayerChanging() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.EXPANSION);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var scoreBoard = ScoreBoard.empty();
        scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
        final var state =
                GameState.BiddingPhase.startRound(dealer, 3, gameRule, dealer, playerIds, scoreBoard);
        state.bid(dealer, 0);
        state.bid(participant, 2);
        final var trickState = state.startTrick();

        // for first trick
        final var greenCard = new Card.NumberCard(new CardId("1"), 10, Card.NumberCard.CardColor.GREEN);
        final var roiseDLaney = new Card.RoiseDLaney(new CardId("2"));

        // for second trick
        final var blackCard = new Card.NumberCard(new CardId("4"), 14, Card.NumberCard.CardColor.BLACK);
        final var pirateCard = new Card.Pirates.StandardPirates(new CardId("3"));

        trickState.getPlayerOf(dealer).setCards(new HashMap<>(Map.of(
                greenCard.getCardId(), greenCard, blackCard.getCardId(), blackCard)));
        trickState.getPlayerOf(participant).setCards(new HashMap<>(Map.of(
                roiseDLaney.getCardId(), roiseDLaney, pirateCard.getCardId(), pirateCard)));

        gameRoom.tell(GameCommand.Store.builder().state(trickState).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        // dealer plays trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(greenCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(dealer).playedCard(greenCard).build());

        // participant plays trick with RoiseDLaney
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(roiseDLaney).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(participant).playedCard(roiseDLaney).build());

        // trick finished
        probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(roiseDLaney).trickBonus(0).build());

        // next lead changeable notice
        probe.expectMessage(GameEvent.NextTrickLeadPlayerChangeableNotice.builder().playerId(participant).build());

        // change next lead
        gameRoom.tell(GameCommand.NextTrickLeadPlayerChange.builder().playerId(participant).newLeadPlayerId(dealer).build());
        probe.expectMessage(GameEvent.NextTrickLeadPlayerChanged.builder().playerId(participant).newLeadPlayerId(dealer).build());

        probe.expectNoMessage();

        // dealer plays 2nd trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(blackCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(dealer).playedCard(blackCard).build());
        // participant plays 2nd trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(pirateCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(participant).playedCard(pirateCard).build());
        // 2nd trick finished
        probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(pirateCard).trickBonus(20).build());
    }

    ///////////////////////////// PlayerHandChanging PHASE /////////////////////////////

    @Test
    public void transitionFromTrickPhaseToPlayerHandChanging() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.EXPANSION);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var scoreBoard = ScoreBoard.empty();
        scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
        final var state =
                GameState.BiddingPhase.startRound(dealer, 3, gameRule, dealer, playerIds, scoreBoard);
        state.bid(dealer, 0);
        state.bid(participant, 2);
        state.getDeck().clear();
        final var deckSkulking = new Card.Skulking(new CardId("sk"));
        state.getDeck().addAll(List.of(deckSkulking, new Card.StandardEscape(new CardId("es"))));
        final var trickState = state.startTrick();

        // for first trick
        final var greenCard = new Card.NumberCard(new CardId("1"), 10, Card.NumberCard.CardColor.GREEN);
        final var bahijTheBandit = new Card.BahijTheBandit(new CardId("2"));

        // for second trick
        final var blackCard = new Card.NumberCard(new CardId("4"), 14, Card.NumberCard.CardColor.BLACK);
        final var pirateCard = new Card.Pirates.StandardPirates(new CardId("3"));

        trickState.getPlayerOf(dealer).setCards(new HashMap<>(Map.of(
                greenCard.getCardId(), greenCard, blackCard.getCardId(), blackCard)));
        trickState.getPlayerOf(participant).setCards(new HashMap<>(Map.of(
                bahijTheBandit.getCardId(), bahijTheBandit, pirateCard.getCardId(), pirateCard)));

        gameRoom.tell(GameCommand.Store.builder().state(trickState).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        // dealer plays trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(greenCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(dealer).playedCard(greenCard).build());

        // participant plays trick with RoiseDLaney
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(bahijTheBandit).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(participant).playedCard(bahijTheBandit).build());

        // trick finished
        probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(bahijTheBandit).trickBonus(0).build());

        // player hand changeable notice
        probe.expectMessage(GameEvent.HandChangeAvailableNotice.builder().playerId(participant)
                .drawCards(List.of(deckSkulking.getCardId(), new CardId("es"))).build());

        // change player hand
        final var returnCards = new HashSet<>(List.of(pirateCard.getCardId(), new CardId("es")));
        gameRoom.tell(GameCommand.PlayerHandChange.builder().playerId(participant).returnCards(returnCards).build());
        probe.expectMessage(GameEvent.PlayerHandChanged.builder().playerId(participant).returnCards(returnCards).build());

        probe.expectNoMessage();

        // participant plays 2nd trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(deckSkulking).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(participant).playedCard(deckSkulking).build());

        // dealer plays 2nd trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(blackCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(dealer).playedCard(blackCard).build());

        // 2nd trick finished
        probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(deckSkulking).trickBonus(20).build());
    }

    ///////////////////////////// BidDeclareChanging PHASE /////////////////////////////

    @Test
    public void transitionFromTrickPhaseToBidDeclareChanging() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.EXPANSION);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var scoreBoard = ScoreBoard.empty();
        scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
        final var state =
                GameState.BiddingPhase.startRound(dealer, 3, gameRule, dealer, playerIds, scoreBoard);
        state.bid(dealer, 0);
        state.bid(participant, 2);
        final var trickState = state.startTrick();

        // for first trick
        final var greenCard = new Card.NumberCard(new CardId("1"), 10, Card.NumberCard.CardColor.GREEN);
        final var harryTheGiant = new Card.HarryTheGiant(new CardId("2"));

        // for second trick
        final var blackCard = new Card.NumberCard(new CardId("4"), 14, Card.NumberCard.CardColor.BLACK);
        final var pirateCard = new Card.Pirates.StandardPirates(new CardId("3"));

        trickState.getPlayerOf(dealer).setCards(new HashMap<>(Map.of(
                greenCard.getCardId(), greenCard, blackCard.getCardId(), blackCard)));
        trickState.getPlayerOf(participant).setCards(new HashMap<>(Map.of(
                harryTheGiant.getCardId(), harryTheGiant, pirateCard.getCardId(), pirateCard)));

        gameRoom.tell(GameCommand.Store.builder().state(trickState).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        // dealer plays trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(greenCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(dealer).playedCard(greenCard).build());

        // participant plays trick with RoiseDLaney
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(harryTheGiant).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(participant).playedCard(harryTheGiant).build());

        // trick finished
        probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(harryTheGiant).trickBonus(0).build());

        // player hand changeable notice
        probe.expectMessage(GameEvent.DeclareBidChangeAvailable.builder().playerId(participant)
                .max(3).min(1).build());

        // change player bid declared
        gameRoom.tell(GameCommand.BidDeclareChange.builder().playerId(participant).bid(1).build());
        probe.expectMessage(GameEvent.BidDeclareChanged.builder().changedPlayerId(participant).changedBid(1).build());

        probe.expectNoMessage();
    }

    ///////////////////////////// FuturePredicating PHASE /////////////////////////////

    @Test
    public void transitionFromTrickPhaseToFuturePredicating() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.EXPANSION);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var scoreBoard = ScoreBoard.empty();
        scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
        final var state =
                GameState.BiddingPhase.startRound(dealer, 3, gameRule, dealer, playerIds, scoreBoard);
        state.bid(dealer, 0);
        state.bid(participant, 2);
        final var currentDeck = new LinkedList<>(List.of(new Card.Skulking(new CardId("sk")), new Card.StandardEscape(new CardId("es"))));
        state.getDeck().clear();
        state.getDeck().addAll(currentDeck);
        final var trickState = state.startTrick();

        // for first trick
        final var greenCard = new Card.NumberCard(new CardId("1"), 10, Card.NumberCard.CardColor.GREEN);
        final var juanitaJade = new Card.JuanitaJade(new CardId("2"));

        // for second trick
        final var blackCard = new Card.NumberCard(new CardId("4"), 14, Card.NumberCard.CardColor.BLACK);
        final var pirateCard = new Card.Pirates.StandardPirates(new CardId("3"));

        trickState.getPlayerOf(dealer).setCards(new HashMap<>(Map.of(
                greenCard.getCardId(), greenCard, blackCard.getCardId(), blackCard)));
        trickState.getPlayerOf(participant).setCards(new HashMap<>(Map.of(
                juanitaJade.getCardId(), juanitaJade, pirateCard.getCardId(), pirateCard)));

        gameRoom.tell(GameCommand.Store.builder().state(trickState).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        // dealer plays trick
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(dealer).card(greenCard).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(dealer).playedCard(greenCard).build());

        // participant plays trick with RoiseDLaney
        gameRoom.tell(GameCommand.PlayCard.builder().playerId(participant).card(juanitaJade).build());
        probe.expectMessage(GameEvent.APlayerTrickPlayed.builder()
                .playerId(participant).playedCard(juanitaJade).build());

        // trick finished
        probe.expectMessage(GameEvent.APlayerWon.builder().winnerId(participant).card(juanitaJade).trickBonus(0).build());

        // predicate-able notice
        probe.expectMessage(GameEvent.FuturePredicateAvailable.builder().playerId(participant)
                .deckCard(currentDeck.stream().map(Card::getCardId).collect(Collectors.toList())).build());

        // finished to predicate
        gameRoom.tell(GameCommand.FuturePredicateFinish.builder().predicatePlayerId(participant).build());
        probe.expectMessage(GameEvent.FuturePredicated.builder().predicatedPlayerId(participant).build());

        probe.expectNoMessage();
    }

    ///////////////////////////// Finished PHASE /////////////////////////////

    @Test
    public void canEndGameOnFinishedPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.EXPANSION);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var scoreBoard = ScoreBoard.empty();
        scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(40, 0), participant, new Score(60, 0)));
        final var finishedPhase = GameState.FinishedPhase.builder()
                .roomOwnerId(dealer)
                .rule(gameRule)
                .lastWinnerId(participant)
                .playerIds(playerIds)
                .scoreBoard(scoreBoard)
                .build();

        gameRoom.tell(GameCommand.Store.builder().state(finishedPhase).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        // end game
        gameRoom.tell(GameCommand.EndGame.builder().playerId(dealer).build());
        probe.expectMessageClass(GameEvent.GameEnded.class);

        // check game room terminated
        final var deadLetterProbe = testKit.createDeadLetterProbe();
        gameRoom.tell(GameCommand.EndGame.builder().playerId(dealer).build());
        deadLetterProbe.expectTerminated(gameRoom);
    }

    @Test
    public void canReplayGameOnFinishedPhase() {
        final ActorRef<GameCommand> gameRoom = testKit.spawn(GameRoomActor.create(newGameRoomId(), dao));
        final TestProbe<GameEvent> probe = testKit.createTestProbe();

        final var dealer = new PlayerId("dealer");
        final var participant = new PlayerId("participant");

        final var gameRule = new GameRule(5, 3, GameRule.DeckType.EXPANSION);
        final var playerIds = new ArrayList<>(List.of(dealer, participant));
        final var scoreBoard = ScoreBoard.empty();
        scoreBoard.addRoundScore(Map.of(dealer, new Score(-10, 0), participant, new Score(-10, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(20, 0), participant, new Score(-20, 0)));
        scoreBoard.addRoundScore(Map.of(dealer, new Score(40, 0), participant, new Score(60, 0)));
        final var finishedPhase = GameState.FinishedPhase.builder()
                .roomOwnerId(dealer)
                .rule(gameRule)
                .lastWinnerId(dealer)
                .playerIds(playerIds)
                .scoreBoard(scoreBoard)
                .build();

        gameRoom.tell(GameCommand.Store.builder().state(finishedPhase).build());
        gameRoom.tell(GameCommand.NewConnection.builder().playerId(dealer).playerRef(probe.getRef()).build());
        probe.expectMessageClass(GameEvent.ConnectionEstablished.class);
        probe.expectMessageClass(GameEvent.GameSnapshot.class);

        // replay game
        gameRoom.tell(GameCommand.ReplayGame.builder().playerId(dealer).build());
        probe.expectMessage(GameEvent.GameReplayed.builder()
                .gameWinnerId(dealer).build());

        final var roundStarted = probe.receiveMessage();
        assertThat(roundStarted).asInstanceOf(InstanceOfAssertFactories.type(GameEvent.RoundStarted.class))
                .satisfies(e -> {
                    final var joinedPlayer = e.getPlayers();
                    assertThat(joinedPlayer.size()).isEqualTo(2);
                    joinedPlayer.forEach(p -> assertThat(p.getCardIds().size()).isEqualTo(1));
                });

        probe.expectMessageClass(GameEvent.BiddingStarted.class);
    }

}