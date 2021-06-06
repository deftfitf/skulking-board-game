package gameserver.service.impl;

import gameserver.domain.CardId;
import gameserver.domain.GameEvent;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GameEventAdapter {

    private final GameRuleAdapter gameRuleAdapter;
    private final CardAdapter cardAdapter;
    private final ScoreBoardAdapter scoreBoardAdapter;
    private final GameStateAdapter gameStateAdapter;

    public gameserver.service.grpc.GameEvent adapt(GameEvent _gameEvent) {
        final var bldr = gameserver.service.grpc.GameEvent.newBuilder();

        if (_gameEvent instanceof GameEvent.Initialized) {
            final var initialized = (GameEvent.Initialized) _gameEvent;
            final var gameRule = gameRuleAdapter.adapt(initialized.getGameRule());
            bldr.setInitialized(gameserver.service.grpc.GameEvent.Initialized.newBuilder()
                    .setGameRule(gameRule)
                    .setFirstDealerId(initialized.getFirstDealerId().getValue()))
                    .build();
        } else if (_gameEvent instanceof GameEvent.ConnectionEstablished) {
            final var connectionEstablished = (GameEvent.ConnectionEstablished) _gameEvent;
            bldr.setConnectionEstablished(gameserver.service.grpc.GameEvent.ConnectionEstablished.newBuilder()
                    .setPlayerId(connectionEstablished.getPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.ConnectionClosed) {
            final var connectionClosed = (GameEvent.ConnectionClosed) _gameEvent;
            bldr.setConnectionClosed(gameserver.service.grpc.GameEvent.ConnectionClosed.newBuilder()
                    .setPlayerId(connectionClosed.getPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.APlayerJoined) {
            final var aPlayerJoined = (GameEvent.APlayerJoined) _gameEvent;
            bldr.setAPlayerJoined(gameserver.service.grpc.GameEvent.APlayerJoined.newBuilder()
                    .setPlayerId(aPlayerJoined.getPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.APlayerLeft) {
            final var aPlayerLeft = (GameEvent.APlayerLeft) _gameEvent;
            bldr.setAPlayerLeft(gameserver.service.grpc.GameEvent.APlayerLeft.newBuilder()
                    .setPlayerId(aPlayerLeft.getPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.RoomDealerChanged) {
            final var roomDealerChanged = (GameEvent.RoomDealerChanged) _gameEvent;
            bldr.setRoomDealerChanged(gameserver.service.grpc.GameEvent.RoomDealerChanged.newBuilder()
                    .setOldDealer(roomDealerChanged.getOldDealer().getValue())
                    .setNewDealer(roomDealerChanged.getNewDealer().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.GameStarted) {
            final var gameStarted = (GameEvent.GameStarted) _gameEvent;
            bldr.setGameStarted(gameserver.service.grpc.GameEvent.GameStarted.newBuilder()
                    .setPlayerId(gameStarted.getPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.BiddingStarted) {
            bldr.setBiddingStarted(gameserver.service.grpc.GameEvent.BiddingStarted.newBuilder().build());
        } else if (_gameEvent instanceof GameEvent.APlayerBidDeclared) {
            final var bidDeclared = (GameEvent.APlayerBidDeclared) _gameEvent;
            bldr.setAPlayerBidDeclared(gameserver.service.grpc.GameEvent.APlayerBidDeclared.newBuilder()
                    .setPlayerId(bidDeclared.getPlayerId().getValue())
                    .setBidDeclared(bidDeclared.getBidDeclared())
                    .build());
        } else if (_gameEvent instanceof GameEvent.RoundStarted) {
            final var roundStarted = (GameEvent.RoundStarted) _gameEvent;
            final var joinedPlayers = roundStarted.getPlayers().stream()
                    .map(joinedPlayer -> gameserver.service.grpc.GameEvent.RoundStarted.JoinedPlayer.newBuilder()
                            .setPlayerId(joinedPlayer.getPlayerId().getValue())
                            .addAllCardId(joinedPlayer.getCardIds().stream().map(CardId::getId).collect(Collectors.toList()))
                            .build())
                    .collect(Collectors.toList());
            bldr.setRoundStarted(gameserver.service.grpc.GameEvent.RoundStarted.newBuilder()
                    .addAllJoinedPlayers(joinedPlayers)
                    .build());
        } else if (_gameEvent instanceof GameEvent.TrickStarted) {
            bldr.setTrickStarted(gameserver.service.grpc.GameEvent.TrickStarted.newBuilder().build());
        } else if (_gameEvent instanceof GameEvent.APlayerTrickPlayed) {
            final var trickPlayed = (GameEvent.APlayerTrickPlayed) _gameEvent;
            final var playedCard = cardAdapter.adapt(trickPlayed.getPlayedCard());
            bldr.setAPlayerTrickPlayed(gameserver.service.grpc.GameEvent.APlayerTrickPlayed.newBuilder()
                    .setPlayerId(trickPlayed.getPlayerId().getValue())
                    .setPlayedCard(playedCard)
                    .build());
        } else if (_gameEvent instanceof GameEvent.APlayerWon) {
            final var aPlayerWon = (GameEvent.APlayerWon) _gameEvent;
            final var playedCard = cardAdapter.adapt(aPlayerWon.getCard());
            bldr.setAPlayerWon(gameserver.service.grpc.GameEvent.APlayerWon.newBuilder()
                    .setWinnerId(aPlayerWon.getWinnerId().getValue())
                    .setCard(playedCard)
                    .setTrickBonus(aPlayerWon.getTrickBonus())
                    .build());
        } else if (_gameEvent instanceof GameEvent.AllRanAway) {
            final var allRanAway = (GameEvent.AllRanAway) _gameEvent;
            final var playedCard = cardAdapter.adapt(allRanAway.getCard());
            bldr.setAllRanAway(gameserver.service.grpc.GameEvent.AllRanAway.newBuilder()
                    .setWinnerId(allRanAway.getWinnerId().getValue())
                    .setCard(playedCard)
                    .build());
        } else if (_gameEvent instanceof GameEvent.KrakenAppeared) {
            final var krakenAppeared = (GameEvent.KrakenAppeared) _gameEvent;
            bldr.setKrakenAppeared(gameserver.service.grpc.GameEvent.KrakenAppeared.newBuilder()
                    .setMustHaveWon(krakenAppeared.getMustHaveWon().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.DeclareBidChangeAvailable) {
            final var declareBidChangeAvailable = (GameEvent.DeclareBidChangeAvailable) _gameEvent;
            bldr.setDeclareBidChangeAvailable(gameserver.service.grpc.GameEvent.DeclareBidChangeAvailable.newBuilder()
                    .setPlayerId(declareBidChangeAvailable.getPlayerId().getValue())
                    .setMax(declareBidChangeAvailable.getMax())
                    .setMin(declareBidChangeAvailable.getMin())
                    .build());
        } else if (_gameEvent instanceof GameEvent.NextTrickLeadPlayerChangeableNotice) {
            final var nextTrickLeadPlayerChangeableNotice = (GameEvent.NextTrickLeadPlayerChangeableNotice) _gameEvent;
            bldr.setNextTrickLeadPlayerChangeableNotice(gameserver.service.grpc.GameEvent.NextTrickLeadPlayerChangeableNotice.newBuilder()
                    .setPlayerId(nextTrickLeadPlayerChangeableNotice.getPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.HandChangeAvailableNotice) {
            final var handChangeAvailableNotice = (GameEvent.HandChangeAvailableNotice) _gameEvent;
            final var drawCards = handChangeAvailableNotice.getDrawCards()
                    .stream().map(CardId::getId).collect(Collectors.toList());
            bldr.setHandChangeAvailableNotice(gameserver.service.grpc.GameEvent.HandChangeAvailableNotice.newBuilder()
                    .setPlayerId(handChangeAvailableNotice.getPlayerId().getValue())
                    .addAllDrawCards(drawCards)
                    .build());
        } else if (_gameEvent instanceof GameEvent.FuturePredicateAvailable) {
            final var futurePredicateAvailable = (GameEvent.FuturePredicateAvailable) _gameEvent;
            final var deckCards = futurePredicateAvailable.getDeckCard()
                    .stream().map(CardId::getId).collect(Collectors.toList());
            bldr.setFuturePredicateAvailable(gameserver.service.grpc.GameEvent.FuturePredicateAvailable.newBuilder()
                    .setPlayerId(futurePredicateAvailable.getPlayerId().getValue())
                    .addAllDeckCard(deckCards)
                    .build());
        } else if (_gameEvent instanceof GameEvent.RoundFinished) {
            final var roundFinished = (GameEvent.RoundFinished) _gameEvent;
            final var roundScores = scoreBoardAdapter.adapt(roundFinished.getRoundScore());

            bldr.setRoundFinished(gameserver.service.grpc.GameEvent.RoundFinished.newBuilder()
                    .putAllRoundScore(roundScores)
                    .build());
        } else if (_gameEvent instanceof GameEvent.NextTrickLeadPlayerChanged) {
            final var nextTrickLeadPlayerChanged = (GameEvent.NextTrickLeadPlayerChanged) _gameEvent;
            bldr.setNextTrickLeadPlayerChanged(gameserver.service.grpc.GameEvent.NextTrickLeadPlayerChanged.newBuilder()
                    .setPlayerId(nextTrickLeadPlayerChanged.getPlayerId().getValue())
                    .setNewLeadPlayerId(nextTrickLeadPlayerChanged.getNewLeadPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.PlayerHandChanged) {
            final var playerHandChanged = (GameEvent.PlayerHandChanged) _gameEvent;
            final var returnCards = playerHandChanged.getReturnCards()
                    .stream().map(CardId::getId).collect(Collectors.toList());
            bldr.setPlayerHandChanged(gameserver.service.grpc.GameEvent.PlayerHandChanged.newBuilder()
                    .setPlayerId(playerHandChanged.getPlayerId().getValue())
                    .addAllReturnCards(returnCards)
                    .build());
        } else if (_gameEvent instanceof GameEvent.FuturePredicated) {
            final var futurePredicated = (GameEvent.FuturePredicated) _gameEvent;
            bldr.setFuturePredicated(gameserver.service.grpc.GameEvent.FuturePredicated.newBuilder()
                    .setPredicatedPlayerId(futurePredicated.getPredicatedPlayerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.BidDeclareChanged) {
            final var bidDeclareChanged = (GameEvent.BidDeclareChanged) _gameEvent;
            bldr.setBidDeclareChanged(gameserver.service.grpc.GameEvent.BidDeclareChanged.newBuilder()
                    .setChangedPlayerId(bidDeclareChanged.getChangedPlayerId().getValue())
                    .setChangedBid(bidDeclareChanged.getChangedBid())
                    .build());
        } else if (_gameEvent instanceof GameEvent.GameFinished) {
            final var gameFinished = (GameEvent.GameFinished) _gameEvent;
            final var scoreBoard = scoreBoardAdapter.adapt(gameFinished.getScoreBoard());
            bldr.setGameFinished(gameserver.service.grpc.GameEvent.GameFinished.newBuilder()
                    .setGameWinnerId(gameFinished.getGameWinnerId().getValue())
                    .setScoreBoard(scoreBoard)
                    .build());
        } else if (_gameEvent instanceof GameEvent.GameReplayed) {
            final var gameReplay = (GameEvent.GameReplayed) _gameEvent;
            bldr.setGameReplayed(gameserver.service.grpc.GameEvent.GameReplayed.newBuilder()
                    .setGameWinnerId(gameReplay.getGameWinnerId().getValue())
                    .build());
        } else if (_gameEvent instanceof GameEvent.GameEnded) {
            bldr.setGameEnded(gameserver.service.grpc.GameEvent.GameEnded.newBuilder().build());
        } else if (_gameEvent instanceof GameEvent.GameSnapshot) {
            final var snapshot = (GameEvent.GameSnapshot) _gameEvent;
            final var state = gameStateAdapter.adapt(snapshot.getGameState());
            bldr.setGameSnapshot(gameserver.service.grpc.GameEvent.GameSnapshot.newBuilder()
                    .setGameState(state));
        } else if (_gameEvent instanceof GameEvent.GameException) {
            final var gameException = (GameEvent.GameException) _gameEvent;
            final var invalidInputType = gameserver.service.grpc.GameEvent.InvalidInputType.valueOf(gameException.getInvalidInputType().name());
            bldr.setGameException(gameserver.service.grpc.GameEvent.GameException.newBuilder()
                    .setInvalidInputType(invalidInputType)
                    .build());
        } else {
            throw new IllegalArgumentException("unsupported event type detected");
        }

        return bldr.build();
    }

}