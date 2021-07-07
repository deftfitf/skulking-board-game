package gameserver.actor;

import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.SupervisorStrategy;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.cluster.sharding.typed.javadsl.ClusterSharding;
import akka.cluster.sharding.typed.javadsl.Entity;
import akka.cluster.sharding.typed.javadsl.EntityTypeKey;
import akka.japi.function.Procedure;
import akka.persistence.typed.PersistenceId;
import akka.persistence.typed.RecoveryCompleted;
import akka.persistence.typed.SnapshotSelectionCriteria;
import akka.persistence.typed.javadsl.*;
import dynamodbdao.GameRoomDynamoDBDao;
import gameserver.domain.*;
import gameserver.query.GameRoomQueryAdapter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GameRoomActor
        extends EventSourcedBehavior<GameCommand, GameEvent, GameState> {

    private final Map<PlayerId, ActorRef<GameEvent>> activeConnections = new HashMap<>();

    public static final EntityTypeKey<GameCommand> ENTITY_TYPE_KEY =
            EntityTypeKey.create(GameCommand.class, "GameRoomActorCommand");

    private final ActorContext<GameCommand> context;
    private final GameRoomDynamoDBDao gameRoomDynamoDBDao;
    private final String gameRoomId;

    private GameRoomActor(
            ActorContext<GameCommand> context, String gameRoomId,
            GameRoomDynamoDBDao gameRoomDynamoDBDao
    ) {
        super(
                PersistenceId.of(ENTITY_TYPE_KEY.name(), gameRoomId),
                SupervisorStrategy
                        .restartWithBackoff(Duration.ofMillis(200), Duration.ofSeconds(5), 0.1));
        this.gameRoomId = gameRoomId;
        this.context = context;
        this.gameRoomDynamoDBDao = gameRoomDynamoDBDao;
    }

    public static void init(ActorSystem<?> system, GameRoomDynamoDBDao dao) {
        ClusterSharding.get(system)
                .init(Entity.of(
                        ENTITY_TYPE_KEY,
                        entityContext -> GameRoomActor.create(entityContext.getEntityId(), dao)));
    }

    @Override
    public SignalHandler<GameState> signalHandler() {
        return newSignalHandlerBuilder()
                .onSignal(RecoveryCompleted.class, (state, sig) -> {
                    if (state == null) {
                        return;
                    }

                    final var gameRoom = GameRoomQueryAdapter.adapt(gameRoomId, state);
                    gameRoomDynamoDBDao.updateRoom(gameRoom);
                })
                .build();
    }

    public static Behavior<GameCommand> create(String gameRoomId, GameRoomDynamoDBDao dao) {
        return Behaviors.setup(ctx -> EventSourcedBehavior
                .start(
                        Behaviors.supervise(new GameRoomActor(ctx, gameRoomId, dao))
                                .onFailure(SupervisorStrategy.restart()),
                        ctx));
    }

    private void narrowcast(PlayerId playerId, GameEvent gameEvent) {
        final var activeConnection = activeConnections.get(playerId);
        if (activeConnection != null) {
            activeConnection.tell(gameEvent);
        }
    }

    private void broadcast(GameEvent gameEvent) {
        activeConnections.values().forEach(actorRef -> actorRef.tell(gameEvent));
    }

    @Override
    public GameState emptyState() {
        return null;
    }

    @Override
    public CommandHandler<GameCommand, GameEvent, GameState> commandHandler() {
        final var builder = newCommandHandlerBuilder();

        builder.forNullState()
                .onCommand(GameCommand.Init.class, this::onInit)
                .orElse(builder.forAnyState())
                .onAnyCommand(() -> Effect().none().thenStop());

        builder.forAnyState()
                .onCommand(GameCommand.NewConnection.class, this::onNewConnection)
                .onCommand(GameCommand.CloseConnection.class, this::onCloseConnection)
                .onCommand(GameCommand.SnapshotRequest.class, this::onSnapshotRequest)
                .onCommand(GameCommand.Store.class, store -> Effect().persist(GameEvent.Stored.builder().state(store.getState()).build()));

        builder.forStateType(GameState.StartPhase.class)
                .onCommand(GameCommand.Join.class, this::onJoin)
                .onCommand(GameCommand.Leave.class, this::onLeft)
                .onCommand(GameCommand.GameStart.class, this::onStart);

        builder.forStateType(GameState.BiddingPhase.class)
                .onCommand(GameCommand.BidDeclare.class, this::onBidDeclare);

        builder.forStateType(GameState.TrickPhase.class)
                .onCommand(GameCommand.PlayCard.class, this::onPlayCard);

        builder.forStateType(GameState.NextTrickLeadPlayerChanging.class)
                .onCommand(GameCommand.NextTrickLeadPlayerChange.class, this::onNextTrickLeadPlayerChange);

        builder.forStateType(GameState.HandChangeWaiting.class)
                .onCommand(GameCommand.PlayerHandChange.class, this::onPlayerHandChange);

        builder.forStateType(GameState.FuturePredicateWaiting.class)
                .onCommand(GameCommand.FuturePredicateFinish.class, this::onFuturePredicateFinish);

        builder.forStateType(GameState.BidDeclareChangeWaiting.class)
                .onCommand(GameCommand.BidDeclareChange.class, this::onBidDeclareChange);

        builder.forStateType(GameState.FinishedPhase.class)
                .onCommand(GameCommand.ReplayGame.class, this::onReplayGame)
                .onCommand(GameCommand.EndGame.class, this::onEndGame);

        return builder.build();
    }

    private Effect<GameEvent, GameState> onInit(GameCommand.Init init) {
        final var initialized = GameEvent.Initialized.builder()
                .gameRoomId(gameRoomId)
                .gameRule(init.getGameRule())
                .firstDealerId(init.getFirstDealerId())
                .build();
        return Effect()
                .persist(initialized)
                .thenRun(newState -> {
                    final var gameRoom = GameRoomQueryAdapter.adapt(gameRoomId, newState);
                    gameRoomDynamoDBDao.putNewRoom(gameRoom);
                })
                .thenReply(init.getResponse(), notUsed -> initialized);
    }

    private Effect<GameEvent, GameState> onNewConnection(GameState state, GameCommand.NewConnection connection) {
        return Effect().none()
                .thenRun(newState -> {
                    addConnection(connection.getPlayerId(), connection.getPlayerRef());
                    narrowcast(connection.getPlayerId(), GameEvent.GameSnapshot.builder().gameRoomId(gameRoomId).gameState(newState).build());
                });
    }

    private Effect<GameEvent, GameState> onCloseConnection(GameState state, GameCommand.CloseConnection connection) {
        return Effect().none()
                .thenRun(() -> {
                    removeConnection(connection.getPlayerId(), connection.getPlayerRef());
                });
    }

    private void addConnection(PlayerId playerId, ActorRef<GameEvent> ref) {
        final var currentRef = activeConnections.get(playerId);
        if (!ref.equals(currentRef)) {
            activeConnections.put(playerId, ref);
            ref.tell(GameEvent.ConnectionEstablished.builder().playerId(playerId).build());
        }
    }

    private void removeConnection(PlayerId playerId, ActorRef<GameEvent> ref) {
        final var currentRef = activeConnections.get(playerId);
        if (ref.equals(currentRef)) {
            activeConnections.remove(playerId, currentRef);
            currentRef.tell(GameEvent.ConnectionClosed.builder().playerId(playerId).build());
        }
    }

    private Effect<GameEvent, GameState> onSnapshotRequest(GameState state, GameCommand.SnapshotRequest snapshotRequest) {
        return Effect().none()
                .thenRun((newState) -> narrowcast(
                        snapshotRequest.getPlayerId(),
                        GameEvent.GameSnapshot.builder().gameRoomId(gameRoomId).gameState(newState).build()));
    }

    private Effect<GameEvent, GameState> whenInvalidInput(InputCheckResult.InvalidInput invalidInput, PlayerId sender) {
        final var activeConnection = activeConnections.get(sender);
        if (activeConnection == null) {
            return Effect().none();
        }
        return whenInvalidInput(sender, invalidInput, activeConnection);
    }

    private Effect<GameEvent, GameState> whenInvalidInput(PlayerId playerId, InputCheckResult.InvalidInput invalidInput, ActorRef<GameEvent> sender) {
        final var event = GameEvent.GameException.builder()
                .playerId(playerId)
                .invalidInputType(invalidInput.getInvalidInputType())
                .build();

        return Effect().none()
                .thenRun(() -> sender.tell(event));
    }

    private Effect<GameEvent, GameState> onJoin(GameState.StartPhase state, GameCommand.Join join) {
        final var joinResult = state.canJoin(join.getPlayerId());
        if (joinResult.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var joined = GameEvent.APlayerJoined.builder()
                    .playerId(join.getPlayerId())
                    .build();

            return Effect()
                    .persist(joined)
                    .thenRun(newState -> {
                        addConnection(join.getPlayerId(), join.getPlayerRef());

                        final var gameRoom = GameRoomQueryAdapter.adapt(gameRoomId, newState);
                        gameRoomDynamoDBDao.updateRoom(gameRoom);

                        narrowcast(joined.getPlayerId(), GameEvent.GameSnapshot.builder().gameRoomId(gameRoomId).gameState(newState).build());
                        broadcast(joined);
                    });

        } else if (joinResult instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput(join.getPlayerId(), (InputCheckResult.InvalidInput) joinResult, join.getPlayerRef());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onLeft(GameState.StartPhase state, GameCommand.Leave leave) {
        final var canLeaveResult = state.canLeave(leave.getPlayerId());
        if (canLeaveResult.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var oldDealerId = state.getDealerId();
            final var left = GameEvent.APlayerLeft.builder()
                    .playerId(leave.getPlayerId())
                    .build();

            final Procedure<GameState> effect = leftState -> {
                final var gameRoom = GameRoomQueryAdapter.adapt(gameRoomId, leftState);
                gameRoomDynamoDBDao.updateRoom(gameRoom);

                final var newDealerId = ((GameState.StartPhase) leftState).getDealerId();
                if (newDealerId.equals(oldDealerId)) {
                    broadcast(left);
                } else {
                    final var dealerChanged = GameEvent.RoomDealerChanged.builder()
                            .oldDealer(oldDealerId)
                            .newDealer(newDealerId)
                            .build();
                    broadcast(dealerChanged);
                }

                removeConnection(leave.getPlayerId(), leave.getPlayerRef());
            };

            if (state.getPlayerIds().size() <= 1) {
                return Effect()
                        .persist(left)
                        .thenRun(effect)
                        .thenStop();
            }

            return Effect()
                    .persist(left)
                    .thenRun(effect);
        } else if (canLeaveResult instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput(leave.getPlayerId(), (InputCheckResult.InvalidInput) canLeaveResult, leave.getPlayerRef());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onStart(GameState.StartPhase state, GameCommand.GameStart start) {
        if (!state.getDealerId().equals(start.getPlayerId())) {
            final var event = GameEvent.GameException.builder()
                    .playerId(start.getPlayerId())
                    .invalidInputType(InputCheckResult.InvalidInputType.FAILED_START_GAME_NOT_DEALER)
                    .build();

            return Effect().none()
                    .thenRun(() -> narrowcast(start.getPlayerId(), event));
        }

        final var canStartBidResult = state.canStartBid();
        if (canStartBidResult.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var gameStarted = GameEvent.GameStarted.builder()
                    .playerIds(state.getPlayerIds())
                    .build();

            return Effect()
                    .persist(gameStarted)
                    .thenRun(newState -> {
                        final var gameRoom = GameRoomQueryAdapter.adapt(gameRoomId, newState);
                        gameRoomDynamoDBDao.updateRoom(gameRoom);

                        broadcast(gameStarted);
                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else if (canStartBidResult instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput((InputCheckResult.InvalidInput) canStartBidResult, start.getPlayerId());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onBidDeclare(GameState.BiddingPhase state, GameCommand.BidDeclare bidDeclare) {
        final var canBid = state.canBid(bidDeclare.getPlayerId(), bidDeclare.getBid());
        if (canBid.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var bidDeclared = GameEvent.APlayerBidDeclared.builder()
                    .playerId(bidDeclare.getPlayerId())
                    .bidDeclared(bidDeclare.getBid())
                    .build();

            return Effect()
                    .persist(bidDeclared)
                    .thenRun(newState -> {
                        broadcast(bidDeclared);
                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else if (canBid instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput((InputCheckResult.InvalidInput) canBid, bidDeclare.getPlayerId());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onPlayCard(GameState.TrickPhase state, GameCommand.PlayCard playCard) {
        final var canPlayCard = state.canPlay(playCard.getPlayerId(), playCard.getCard());
        if (canPlayCard.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var played = GameEvent.APlayerTrickPlayed.builder()
                    .playerId(playCard.getPlayerId())
                    .playedCard(playCard.getCard())
                    .build();

            return Effect()
                    .persist(played)
                    .thenRun(newState -> {
                        if (newState.getStateName() == GameStateType.GAME_FINISHED) {
                            final var gameRoom = GameRoomQueryAdapter.adapt(gameRoomId, newState);
                            gameRoomDynamoDBDao.updateRoom(gameRoom);
                        }

                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else if (canPlayCard instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput((InputCheckResult.InvalidInput) canPlayCard, playCard.getPlayerId());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onNextTrickLeadPlayerChange(GameState.NextTrickLeadPlayerChanging state, GameCommand.NextTrickLeadPlayerChange nextTrickLeadPlayerChange) {
        final var canNextTrickLeadPlayerChange = state.canChangeLeadPlayer(
                nextTrickLeadPlayerChange.getPlayerId(),
                nextTrickLeadPlayerChange.getNewLeadPlayerId());
        if (canNextTrickLeadPlayerChange.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var nextTrickLeadPlayerChanged = GameEvent.NextTrickLeadPlayerChanged.builder()
                    .playerId(nextTrickLeadPlayerChange.getPlayerId())
                    .newLeadPlayerId(nextTrickLeadPlayerChange.getNewLeadPlayerId())
                    .build();

            return Effect()
                    .persist(nextTrickLeadPlayerChanged)
                    .thenRun(newState -> {
                        broadcast(nextTrickLeadPlayerChanged);
                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else if (canNextTrickLeadPlayerChange instanceof InputCheckResult.InvalidInput) {
            whenInvalidInput((InputCheckResult.InvalidInput) canNextTrickLeadPlayerChange, nextTrickLeadPlayerChange.getPlayerId());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onPlayerHandChange(GameState.HandChangeWaiting state, GameCommand.PlayerHandChange playerHandChange) {
        final var canPlayerHandChange = state.canChangeHand(playerHandChange.getReturnCards());
        if (canPlayerHandChange.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var playerHandChanged = GameEvent.PlayerHandChanged.builder()
                    .playerId(playerHandChange.getPlayerId())
                    .returnCards(playerHandChange.getReturnCards())
                    .build();

            return Effect()
                    .persist(playerHandChanged)
                    .thenRun(newState -> {
                        broadcast(playerHandChanged);
                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else if (canPlayerHandChange instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput((InputCheckResult.InvalidInput) canPlayerHandChange, playerHandChange.getPlayerId());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onFuturePredicateFinish(GameState.FuturePredicateWaiting state, GameCommand.FuturePredicateFinish futurePredicateFinish) {
        if (state.getPredicatingPlayerId().equals(futurePredicateFinish.getPredicatePlayerId())) {
            final var futurePredicated = GameEvent.FuturePredicated.builder()
                    .predicatedPlayerId(futurePredicateFinish.getPredicatePlayerId())
                    .build();

            return Effect()
                    .persist(futurePredicated)
                    .thenRun(newState -> {
                        broadcast(futurePredicated);
                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else {
            final var exception = InputCheckResult.InvalidInput.builder()
                    .invalidInputType(InputCheckResult.InvalidInputType.ILLEGAL_PLAYER_ACTION_DETECTED)
                    .build();

            return whenInvalidInput(exception, futurePredicateFinish.getPredicatePlayerId());
        }
    }

    private Effect<GameEvent, GameState> onBidDeclareChange(GameState.BidDeclareChangeWaiting state, GameCommand.BidDeclareChange bidDeclareChange) {
        final var canBidDeclareChange = state.canChangeBid(bidDeclareChange.getPlayerId(), bidDeclareChange.getBid());
        if (canBidDeclareChange.equals(InputCheckResult.ApplyableInput.INSTANCE)) {
            final var bidDeclareChanged = GameEvent.BidDeclareChanged.builder()
                    .changedPlayerId(bidDeclareChange.getPlayerId())
                    .changedBid(bidDeclareChange.getBid())
                    .build();

            return Effect()
                    .persist(bidDeclareChanged)
                    .thenRun(newState -> {
                        broadcast(bidDeclareChanged);
                        newState.getEventQueue().forEach(this::broadcast);
                    });
        } else if (canBidDeclareChange instanceof InputCheckResult.InvalidInput) {
            return whenInvalidInput((InputCheckResult.InvalidInput) canBidDeclareChange, bidDeclareChange.getPlayerId());
        }

        return Effect().unhandled();
    }

    private Effect<GameEvent, GameState> onReplayGame(GameState.FinishedPhase state, GameCommand.ReplayGame replayGame) {
        final var gameReplayed = GameEvent.GameReplayed.builder()
                .gameWinnerId(state.getGameWinnerId())
                .build();

        return Effect()
                .persist(gameReplayed)
                .thenRun(biddingPhase -> {
                    broadcast(gameReplayed);
                    biddingPhase.getEventQueue().forEach(this::broadcast);
                });
    }

    private Effect<GameEvent, GameState> onEndGame(GameState.FinishedPhase state, GameCommand.EndGame endGame) {
        final var gameEnded = GameEvent.GameEnded.builder().build();
        return Effect()
                .persist(gameEnded)
                .thenRun(() -> {
                    gameRoomDynamoDBDao.deleteRoom(gameRoomId);

                    broadcast(gameEnded);
                })
                .thenStop();
    }

    @Override
    public EventHandler<GameState, GameEvent> eventHandler() {
        final var builder = newEventHandlerBuilder();

        builder.forNullState()
                .onEvent(GameEvent.Initialized.class, initialized ->
                        GameState.StartPhase.empty(
                                initialized.getGameRule(),
                                initialized.getFirstDealerId()));

        builder.forAnyState()
                .onEvent(GameEvent.Stored.class, GameEvent.Stored::getState);

        builder.forStateType(GameState.StartPhase.class)
                .onEvent(GameEvent.APlayerJoined.class, this::applyAPlayerJoined)
                .onEvent(GameEvent.APlayerLeft.class, this::applyAPlayerLeft)
                .onEvent(GameEvent.GameStarted.class, this::applyGameStarted);

        builder.forStateType(GameState.BiddingPhase.class)
                .onEvent(GameEvent.APlayerBidDeclared.class, this::applyAPlayerBidDeclared);

        builder.forStateType(GameState.TrickPhase.class)
                .onEvent(GameEvent.APlayerTrickPlayed.class, this::applyAPlayerTrickPlayed);

        builder.forStateType(GameState.NextTrickLeadPlayerChanging.class)
                .onEvent(GameEvent.NextTrickLeadPlayerChanged.class, this::applyNextTrickLeadPlayerChanged);

        builder.forStateType(GameState.HandChangeWaiting.class)
                .onEvent(GameEvent.PlayerHandChanged.class, this::applyHandChanged);

        builder.forStateType(GameState.FuturePredicateWaiting.class)
                .onEvent(GameEvent.FuturePredicated.class, this::applyFuturePredicated);

        builder.forStateType(GameState.BidDeclareChangeWaiting.class)
                .onEvent(GameEvent.BidDeclareChanged.class, this::applyBidDeclareChanged);

        builder.forStateType(GameState.FinishedPhase.class)
                .onEvent(GameEvent.GameEnded.class, (s, e) -> s)
                .onEvent(GameEvent.GameReplayed.class, this::applyGameReplayed);

        return builder.build();
    }

    private GameState applyAPlayerJoined(GameState.StartPhase state, GameEvent.APlayerJoined aPlayerJoined) {
        state.join(aPlayerJoined.getPlayerId());

        return state;
    }

    private GameState applyAPlayerLeft(GameState.StartPhase state, GameEvent.APlayerLeft aPlayerLeft) {
        state.leave(aPlayerLeft.getPlayerId());

        return state;
    }

    private GameState applyGameStarted(GameState.StartPhase state, GameEvent.GameStarted gameStarted) {
        final var biddingPhase = state.startBidding();
        biddingPhase.addGameEvent(extractRoundStartedEvent(biddingPhase));
        biddingPhase.addGameEvent(GameEvent.BiddingStarted.builder()
                .round(1)
                .dealerId(state.getDealerId())
                .build());
        return biddingPhase;
    }

    private GameState applyAPlayerBidDeclared(GameState.BiddingPhase state, GameEvent.APlayerBidDeclared aPlayerBidDeclared) {
        state.bid(aPlayerBidDeclared.getPlayerId(), aPlayerBidDeclared.getBidDeclared());

        if (state.canStartRound()) {
            final var trickPhase = state.startTrick();
            trickPhase.addGameEvent(GameEvent.TrickStarted.builder()
                    .deck(trickPhase.getDeck().size())
                    .trick(trickPhase.getTrick())
                    .players(trickPhase.getPlayers())
                    .build());
            return trickPhase;
        }

        return state;
    }

    private GameState applyAPlayerTrickPlayed(GameState.TrickPhase state, GameEvent.APlayerTrickPlayed aPlayerTrickPlayed) {
        state.play(aPlayerTrickPlayed.getPlayerId(), aPlayerTrickPlayed.getPlayedCard());

        state.clearEventQueue();
        state.addGameEvent(aPlayerTrickPlayed);

        if (state.isFinishedTrick()) {
            final var trickResult = state.judge();

            if (trickResult instanceof GameState.TrickPhase.APlayerWon) {
                final var aPlayerWon = (GameState.TrickPhase.APlayerWon) trickResult;
                state.addGameEvent(GameEvent.APlayerWon.builder()
                        .winnerId(aPlayerWon.getWinnerId())
                        .trickBonus(aPlayerWon.getTrickBonus())
                        .card(aPlayerWon.getCard())
                        .build());

                final var newState = handlePiratesEvent(state, (GameState.TrickPhase.APlayerWon) trickResult);
                if (newState != null) {
                    return newState;
                }
            } else if (trickResult instanceof GameState.TrickPhase.AllRanAway) {
                final var allRanAway = (GameState.TrickPhase.AllRanAway) trickResult;
                state.addGameEvent(GameEvent.AllRanAway.builder()
                        .winnerId(allRanAway.getWinnerId())
                        .card(allRanAway.getCard())
                        .build());
            } else if (trickResult instanceof GameState.TrickPhase.KrakenAppeared) {
                final var krakenAppeared = (GameState.TrickPhase.KrakenAppeared) trickResult;
                state.addGameEvent(GameEvent.KrakenAppeared.builder()
                        .mustHaveWon(krakenAppeared.getWinnerId())
                        .build());
            }

            return settleTrick(state, trickResult);
        }

        return state;
    }

    private GameState settleTrick(GameState.TrickPhase state, GameState.TrickPhase.TrickFinishResult trickFinishResult) {
        state.prepareNextTrick(trickFinishResult);
        if (state.isRoundFinished()) {
            state.calcRoundScore();
            state.addGameEvent(GameEvent.RoundFinished.builder()
                    .roundScore(state.getScoreBoard().getLastRoundScore())
                    .build());

            if (state.isGameFinished()) {
                state.addGameEvent(GameEvent.GameFinished.builder()
                        .gameWinnerId(state.getDealerId())
                        .scoreBoard(state.getScoreBoard())
                        .build());

                final var finishedPhase = state.finish();
                state.getEventQueue().forEach(finishedPhase::addGameEvent);
                return finishedPhase;
            }

            final var biddingPhase = state.nextBiddingPhase();
            state.getEventQueue().forEach(biddingPhase::addGameEvent);
            biddingPhase.addGameEvent(extractRoundStartedEvent(biddingPhase));
            biddingPhase.addGameEvent(GameEvent.BiddingStarted.builder()
                    .round(biddingPhase.getRound())
                    .dealerId(biddingPhase.getDealerId())
                    .build());
            return biddingPhase;
        }

        return state;
    }

    private GameEvent.RoundStarted extractRoundStartedEvent(GameState.BiddingPhase biddingPhase) {
        final var joinedPlayers = biddingPhase.getPlayerIds().stream()
                .map(playerId -> biddingPhase.getIdToPlayer().get(playerId))
                .map(player -> GameEvent.RoundStarted.JoinedPlayer.builder()
                        .playerId(player.getPlayerId())
                        .cardIds(new ArrayList<>(player.getCards().keySet()))
                        .build())
                .collect(Collectors.toList());

        return GameEvent.RoundStarted.builder()
                .round(biddingPhase.getRound())
                .deck(biddingPhase.getDeck().size())
                .players(joinedPlayers).build();
    }

    private GameState handlePiratesEvent(GameState.TrickPhase state, GameState.TrickPhase.APlayerWon aPlayerWon) {
        final var piratesEvent = aPlayerWon.getPiratesEvent();
        if (piratesEvent == null) {
            return null;
        }

        if (piratesEvent instanceof PiratesEvent.DeclareBidChangeAvailable) {
            final var declareBidChangeAvailable = (PiratesEvent.DeclareBidChangeAvailable) piratesEvent;
            state.addGameEvent(GameEvent.DeclareBidChangeAvailable.builder()
                    .playerId(declareBidChangeAvailable.getPlayerId())
                    .min(declareBidChangeAvailable.getMin())
                    .max(declareBidChangeAvailable.getMax())
                    .build());

            return state.bidDeclareChangeWaiting((PiratesEvent.DeclareBidChangeAvailable) piratesEvent, aPlayerWon);
        }

        if (state.isRoundFinished()) {
            return null;
        }

        if (piratesEvent instanceof PiratesEvent.NextTrickLeadPlayerChangeableNotice) {
            final var nextTrickLeadPlayerChangeableNotice = (PiratesEvent.NextTrickLeadPlayerChangeableNotice) piratesEvent;
            state.addGameEvent(GameEvent.NextTrickLeadPlayerChangeableNotice.builder()
                    .playerId(nextTrickLeadPlayerChangeableNotice.getPlayerId())
                    .build());

            return state.nextTrickLeadPlayerChanging(
                    (PiratesEvent.NextTrickLeadPlayerChangeableNotice) piratesEvent, aPlayerWon);
        } else if (piratesEvent instanceof PiratesEvent.HandChangeAvailableNotice) {
            final var handChangeAvailableNotice = (PiratesEvent.HandChangeAvailableNotice) piratesEvent;
            state.addGameEvent(GameEvent.HandChangeAvailableNotice.builder()
                    .playerId(handChangeAvailableNotice.getPlayerId())
                    .drawCards(handChangeAvailableNotice.getDrawCards())
                    .build());

            return state.handChangeWaiting(
                    (PiratesEvent.HandChangeAvailableNotice) piratesEvent, aPlayerWon);
        } else if (piratesEvent instanceof PiratesEvent.FuturePredicateAvailable) {
            final var futurePredicateAvailable = (PiratesEvent.FuturePredicateAvailable) piratesEvent;
            state.addGameEvent(GameEvent.FuturePredicateAvailable.builder()
                    .playerId(futurePredicateAvailable.getPlayerId())
                    .deckCard(state.getDeck().stream().map(Card::getCardId).collect(Collectors.toList()))
                    .build());

            return state.futurePredicateWaiting(
                    (PiratesEvent.FuturePredicateAvailable) piratesEvent, aPlayerWon);
        }

        return null;
    }

    private GameState applyNextTrickLeadPlayerChanged(GameState.NextTrickLeadPlayerChanging state, GameEvent.NextTrickLeadPlayerChanged nextTrickLeadPlayerChanged) {
        state.getTrickPhase().clearEventQueue();
        final var nextState = settleTrick(state.getTrickPhase(), state.getAPlayerWon());
        if (nextState instanceof GameState.TrickPhase) {
            final var trickPhase = (GameState.TrickPhase) nextState;
            trickPhase.setDealerId(nextTrickLeadPlayerChanged.getNewLeadPlayerId());
            trickPhase.rotatePlayers(nextTrickLeadPlayerChanged.getNewLeadPlayerId());
        }
        return nextState;
    }

    private GameState applyFuturePredicated(GameState.FuturePredicateWaiting state, GameEvent.FuturePredicated futurePredicated) {
        state.getTrickPhase().clearEventQueue();
        return settleTrick(state.getTrickPhase(), state.getAPlayerWon());
    }

    private GameState applyHandChanged(GameState.HandChangeWaiting state, GameEvent.PlayerHandChanged playerHandChanged) {
        state.changeHand(playerHandChanged.getReturnCards());
        state.getTrickPhase().clearEventQueue();
        return settleTrick(state.getTrickPhase(), state.getAPlayerWon());
    }

    private GameState applyBidDeclareChanged(GameState.BidDeclareChangeWaiting state, GameEvent.BidDeclareChanged bidDeclareChanged) {
        state.changeBid(bidDeclareChanged.getChangedPlayerId(), bidDeclareChanged.getChangedBid());
        state.getTrickPhase().clearEventQueue();
        return settleTrick(state.getTrickPhase(), state.getAPlayerWon());
    }

    private GameState applyGameReplayed(GameState.FinishedPhase state, GameEvent.GameReplayed gameReplayed) {
        final var biddingPhase = state.replayGame();
        biddingPhase.addGameEvent(extractRoundStartedEvent(biddingPhase));
        biddingPhase.addGameEvent(GameEvent.BiddingStarted.builder()
                .round(biddingPhase.getRound())
                .dealerId(biddingPhase.getDealerId())
                .build());
        return biddingPhase;
    }

    @Override
    public SnapshotSelectionCriteria snapshotSelectionCriteria() {
        return SnapshotSelectionCriteria.latest();
    }

    @Override
    public boolean shouldSnapshot(GameState gameState, GameEvent event, long sequenceNr) {
        return super.shouldSnapshot(gameState, event, sequenceNr);
    }

    @Override
    public RetentionCriteria retentionCriteria() {
        return RetentionCriteria.snapshotEvery(100, 2);
    }

    @Override
    public Recovery recovery() {
        return super.recovery();
    }

}
