package gameserver.service.impl;

import gameserver.domain.CardId;
import gameserver.domain.GameState;
import gameserver.domain.PlayerId;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GameStateAdapter {

    private final GameRuleAdapter gameRuleAdapter;
    private final CardAdapter cardAdapter;
    private final ScoreBoardAdapter scoreBoardAdapter;

    public gameserver.service.grpc.GameState adapt(String gameRoomId, PlayerId myPlayerId, GameState gameState) {
        final var deck = gameState.getRule().provideNewDeck().stream()
                .map(cardAdapter::adapt)
                .collect(Collectors.toList());

        final var bldr = gameserver.service.grpc.GameState.newBuilder();
        bldr.setGameRoomId(gameRoomId);
        bldr.setRoomOwnerId(gameState.getRoomOwnerId().getValue());
        bldr.addAllDeck(deck);

        if (gameState instanceof GameState.StartPhase) {
            final var startPhase = (GameState.StartPhase) gameState;
            final var gameRule = gameRuleAdapter.adapt(startPhase.getRule());
            final var allPlayerIds = startPhase.getPlayerIds()
                    .stream().map(PlayerId::getValue).collect(Collectors.toList());

            bldr.setStartPhase(gameserver.service.grpc.GameState.StartPhase.newBuilder()
                    .setGameRule(gameRule)
                    .setDealerId(startPhase.getDealerId().getValue())
                    .addAllPlayerIds(allPlayerIds));
        } else if (gameState instanceof GameState.BiddingPhase) {
            final var biddingPhase = (GameState.BiddingPhase) gameState;
            final var gameRule = gameRuleAdapter.adapt(biddingPhase.getRule());
            final var biddingPlayers = biddingPhase.getPlayerIds()
                    .stream().map(playerId -> {
                        final var player = biddingPhase.getIdToPlayer().get(playerId);
                        final var isBid = player != null && player.getDeclaredBid() != null;
                        return gameserver.service.grpc.GameState.BiddingPhase.BiddingPlayer.newBuilder()
                                .setPlayerId(playerId.getValue())
                                .setIsBid(isBid)
                                .setCard(player.getCards().size())
                                .build();
                    }).collect(Collectors.toList());
            final var myPlayer = biddingPhase.getIdToPlayer().get(myPlayerId);
            final var cards = myPlayer
                    .getCards().keySet().stream().map(CardId::getId)
                    .collect(Collectors.toList());

            bldr.setBiddingPhase(gameserver.service.grpc.GameState.BiddingPhase.newBuilder()
                    .setGameRule(gameRule)
                    .setRound(biddingPhase.getRound())
                    .setDeck(biddingPhase.getDeck().size())
                    .setDealerId(biddingPhase.getDealerId().getValue())
                    .addAllBiddingPlayers(biddingPlayers)
                    .addAllCard(cards)
                    .setMyBid(myPlayer.getDeclaredBid() != null ? myPlayer.getDeclaredBid() : -1)
                    .setScoreBoard(scoreBoardAdapter.adapt(biddingPhase.getScoreBoard()))
                    .build());
        } else if (gameState instanceof GameState.TrickPhase) {
            final var trickPhase = (GameState.TrickPhase) gameState;

            bldr.setTrickPhase(adapt(myPlayerId, trickPhase));
        } else if (gameState instanceof GameState.NextTrickLeadPlayerChanging) {
            final var changing = (GameState.NextTrickLeadPlayerChanging) gameState;
            bldr.setNextTrickLeadPlayerChanging(gameserver.service.grpc.GameState.NextTrickLeadPlayerChanging.newBuilder()
                    .setChangingPlayerId(changing.getChangingPlayerId().getValue())
                    .setTrickPhase(adapt(myPlayerId, changing.getTrickPhase()))
                    .build());
        } else if (gameState instanceof GameState.HandChangeWaiting) {
            final var waiting = (GameState.HandChangeWaiting) gameState;
            bldr.setHandChangeWaiting(gameserver.service.grpc.GameState.HandChangeWaiting.newBuilder()
                    .setChangingPlayerId(waiting.getChangingPlayerId().getValue())
                    .setTrickPhase(adapt(myPlayerId, waiting.getTrickPhase()))
                    .build());
        } else if (gameState instanceof GameState.FuturePredicateWaiting) {
            final var waiting = (GameState.FuturePredicateWaiting) gameState;
            bldr.setFuturePredicateWaiting(gameserver.service.grpc.GameState.FuturePredicateWaiting.newBuilder()
                    .setPredicatingPlayerId(waiting.getPredicatingPlayerId().getValue())
                    .setTrickPhase(adapt(myPlayerId, waiting.getTrickPhase()))
                    .build());
        } else if (gameState instanceof GameState.BidDeclareChangeWaiting) {
            final var waiting = (GameState.BidDeclareChangeWaiting) gameState;
            bldr.setBidDeclareChangeWaiting(gameserver.service.grpc.GameState.BidDeclareChangeWaiting.newBuilder()
                    .setChangingPlayerId(waiting.getChangingPlayerId().getValue())
                    .setTrickPhase(adapt(myPlayerId, waiting.getTrickPhase()))
                    .build());
        } else if (gameState instanceof GameState.FinishedPhase) {
            final var finished = (GameState.FinishedPhase) gameState;
            final var gameRule = gameRuleAdapter.adapt(finished.getRule());
            final var playerIds = finished.getPlayerIds()
                    .stream().map(PlayerId::getValue).collect(Collectors.toList());
            final var scoreBoard = scoreBoardAdapter.adapt(finished.getScoreBoard());
            bldr.setFinishedPhase(gameserver.service.grpc.GameState.FinishedPhase.newBuilder()
                    .setGameRule(gameRule)
                    .setLastWinnerId(finished.getLastWinnerId().getValue())
                    .addAllPlayerIds(playerIds)
                    .setScoreBoard(scoreBoard)
                    .build());
        } else {
            throw new RuntimeException("unexpected game-state detected");
        }

        return bldr.build();
    }

    private gameserver.service.grpc.GameState.TrickPhase adapt(
            PlayerId playerId,
            GameState.TrickPhase trickPhase
    ) {
        final var gameRule = gameRuleAdapter.adapt(trickPhase.getRule());
        final var trickPlayers = trickPhase.getPlayers()
                .stream().map(player -> gameserver.service.grpc.GameState.TrickPhase.TrickPlayer.newBuilder()
                        .setPlayerId(player.getPlayerId().getValue())
                        .setDeclaredBid(player.getDeclaredBid())
                        .setTookTrick(player.getTookTrick())
                        .setTookBonus(player.getTookBonus())
                        .setCard(player.getCards().size())
                        .build())
                .collect(Collectors.toList());
        final var field = trickPhase.getField()
                .stream().map(playedCard -> gameserver.service.grpc.GameState.TrickPhase.PlayedCard.newBuilder()
                        .setPlayerId(playedCard.getPlayerId().getValue())
                        .setCard(cardAdapter.adapt(playedCard.getCard()))
                        .build())
                .collect(Collectors.toList());
        final var scoreBoard = scoreBoardAdapter.adapt(trickPhase.getScoreBoard());
        final var myPlayer = trickPhase.getPlayerOf(playerId);
        final var cards = myPlayer
                .getCards().keySet().stream().map(CardId::getId)
                .collect(Collectors.toList());

        final var bldr = gameserver.service.grpc.GameState.TrickPhase.newBuilder()
                .setGameRule(gameRule)
                .setRound(trickPhase.getRound())
                .setDealerId(trickPhase.getDealerId().getValue())
                .addAllTrickPlayers(trickPlayers)
                .addAllCard(cards)
                .addAllField(field)
                .setTrick(trickPhase.getTrick())
                .setScoreBoard(scoreBoard)
                .setDeck(trickPhase.getDeck().size())
                .setStack(trickPhase.getStack().size());

        if (trickPhase.getMustFollow() != null) {
            bldr.setMustFollow(gameserver.service.grpc.Card.CardColor.forNumber(trickPhase.getMustFollow().ordinal()));
        }

        return bldr.build();
    }

}
