package gameserver.service.impl;

import gameserver.domain.GameState;
import gameserver.domain.PlayerId;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GameStateAdapter {

    private final GameRuleAdapter gameRuleAdapter;
    private final CardAdapter cardAdapter;
    private final ScoreBoardAdapter scoreBoardAdapter;

    public gameserver.service.grpc.GameState adapt(GameState gameState) {
        final var bldr = gameserver.service.grpc.GameState.newBuilder();

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
                                .build();
                    }).collect(Collectors.toList());

            bldr.setBiddingPhase(gameserver.service.grpc.GameState.BiddingPhase.newBuilder()
                    .setGameRule(gameRule)
                    .setRound(biddingPhase.getRound())
                    .setDealerId(biddingPhase.getDealerId().getValue())
                    .addAllBiddingPlayers(biddingPlayers)
                    .build());
        } else if (gameState instanceof GameState.TrickPhase) {
            final var trickPhase = (GameState.TrickPhase) gameState;
            final var gameRule = gameRuleAdapter.adapt(trickPhase.getRule());
            final var trickPlayers = trickPhase.getPlayers()
                    .stream().map(player -> gameserver.service.grpc.GameState.TrickPhase.TrickPlayer.newBuilder()
                            .setPlayerId(player.getPlayerId().getValue())
                            .setDeclaredBid(player.getDeclaredBid())
                            .setTookTrick(player.getTookTrick())
                            .setTookBonus(player.getTookBonus())
                            .build())
                    .collect(Collectors.toList());
            final var field = trickPhase.getField()
                    .stream().map(playedCard -> gameserver.service.grpc.GameState.TrickPhase.PlayedCard.newBuilder()
                            .setPlayerId(playedCard.getPlayerId().getValue())
                            .setCard(cardAdapter.adapt(playedCard.getCard()))
                            .build())
                    .collect(Collectors.toList());
            final var mustFollow = gameserver.service.grpc.Card.CardColor.valueOf(trickPhase.getMustFollow().name());
            final var scoreBoard = scoreBoardAdapter.adapt(trickPhase.getScoreBoard());

            bldr.setTrickPhase(gameserver.service.grpc.GameState.TrickPhase.newBuilder()
                    .setGameRule(gameRule)
                    .setRound(trickPhase.getRound())
                    .setDealerId(trickPhase.getDealerId().getValue())
                    .addAllTrickPlayers(trickPlayers)
                    .addAllField(field)
                    .setMustFollow(mustFollow)
                    .setTrick(trickPhase.getTrick())
                    .setScoreBoard(scoreBoard)
                    .build());
        } else if (gameState instanceof GameState.NextTrickLeadPlayerChanging) {
            final var changing = (GameState.NextTrickLeadPlayerChanging) gameState;
            bldr.setNextTrickLeadPlayerChanging(gameserver.service.grpc.GameState.NextTrickLeadPlayerChanging.newBuilder()
                    .setChangingPlayerId(changing.getChangingPlayerId().getValue())
                    .build());
        } else if (gameState instanceof GameState.HandChangeWaiting) {
            final var waiting = (GameState.HandChangeWaiting) gameState;
            bldr.setHandChangeWaiting(gameserver.service.grpc.GameState.HandChangeWaiting.newBuilder()
                    .setChangingPlayerId(waiting.getChangingPlayerId().getValue())
                    .build());
        } else if (gameState instanceof GameState.FuturePredicateWaiting) {
            final var waiting = (GameState.FuturePredicateWaiting) gameState;
            bldr.setFuturePredicateWaiting(gameserver.service.grpc.GameState.FuturePredicateWaiting.newBuilder()
                    .setPredicatingPlayerId(waiting.getPredicatingPlayerId().getValue())
                    .build());
        } else if (gameState instanceof GameState.BidDeclareChangeWaiting) {
            final var waiting = (GameState.BidDeclareChangeWaiting) gameState;
            bldr.setBidDeclareChangeWaiting(gameserver.service.grpc.GameState.BidDeclareChangeWaiting.newBuilder()
                    .setChangingPlayerId(waiting.getChangingPlayerId().getValue())
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

}
