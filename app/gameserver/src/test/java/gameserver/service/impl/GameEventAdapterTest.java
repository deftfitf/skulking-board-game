package gameserver.service.impl;

import gameserver.domain.*;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class GameEventAdapterTest {

    private final GameEventAdapter adapter = new GameEventAdapter(
            new GameRuleAdapter(),
            new CardAdapter(),
            new ScoreBoardAdapter(),
            new GameStateAdapter(
                    new GameRuleAdapter(),
                    new CardAdapter(),
                    new ScoreBoardAdapter()));

    @Test
    public void adaptInit() {
        {
            final var event = GameEvent.Initialized.builder()
                    .gameRoomId("gameRoom1")
                    .firstDealerId(new PlayerId("player1"))
                    .gameRule(new GameRule(1, 2, GameRule.DeckType.EXPANSION))
                    .build();

            assertThat(adapter.adapt(event)).satisfies(actual -> {
                assertThat(actual).isNotNull();
                assertThat(actual.getGameRoomId()).isEqualTo("gameRoom1");
                assertThat(actual.getFirstDealerId()).isEqualTo("player1");
                assertThat(actual.getGameRule()).isNotNull();
                assertThat(actual.getGameRule().getRoomSize()).isEqualTo(1);
                assertThat(actual.getGameRule().getNOfRounds()).isEqualTo(2);
                assertThat(actual.getGameRule().getDeckType()).isEqualTo(gameserver.service.grpc.GameRule.DeckType.EXPANSION);
            });
        }
    }

    @Test
    public void adapt() {
        {
            final var event = GameEvent.ConnectionEstablished.builder()
                    .playerId(new PlayerId("player1"))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getConnectionEstablished()).isNotNull();
                assertThat(actual.getConnectionEstablished().getPlayerId()).isEqualTo("player1");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).isNull();
        }
        {
            final var event = GameEvent.ConnectionClosed.builder()
                    .playerId(new PlayerId("player1"))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getConnectionClosed()).isNotNull();
                assertThat(actual.getConnectionClosed().getPlayerId()).isEqualTo("player1");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).isNull();
        }
        {
            final var event = GameEvent.APlayerJoined.builder()
                    .playerId(new PlayerId("player1"))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerJoined()).isNotNull();
                assertThat(actual.getAPlayerJoined().getPlayerId()).isEqualTo("player1");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerJoined()).isNotNull();
                assertThat(actual.getAPlayerJoined().getPlayerId()).isEqualTo("player1");
            });
        }
        {
            final var event = GameEvent.APlayerLeft.builder()
                    .playerId(new PlayerId("player1"))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerLeft()).isNotNull();
                assertThat(actual.getAPlayerLeft().getPlayerId()).isEqualTo("player1");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerLeft()).isNotNull();
                assertThat(actual.getAPlayerLeft().getPlayerId()).isEqualTo("player1");
            });
        }
        {
            final var event = GameEvent.RoomDealerChanged.builder()
                    .oldDealer(new PlayerId("player1"))
                    .newDealer(new PlayerId("player2"))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getRoomDealerChanged()).isNotNull();
                assertThat(actual.getRoomDealerChanged().getOldDealer()).isEqualTo("player1");
                assertThat(actual.getRoomDealerChanged().getNewDealer()).isEqualTo("player2");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getRoomDealerChanged()).isNotNull();
                assertThat(actual.getRoomDealerChanged().getOldDealer()).isEqualTo("player1");
                assertThat(actual.getRoomDealerChanged().getNewDealer()).isEqualTo("player2");
            });
        }
        {
            final var event = GameEvent.GameStarted.builder()
                    .playerIds(List.of(new PlayerId("player1"), new PlayerId("player2")))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getGameStarted()).isNotNull();
                assertThat(actual.getGameStarted().getPlayerIdList()).asList()
                        .containsExactly("player1", "player2");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getGameStarted()).isNotNull();
                assertThat(actual.getGameStarted().getPlayerIdList()).asList()
                        .containsExactly("player1", "player2");
            });
        }
        {
            final var event = GameEvent.BiddingStarted.builder()
                    .dealerId(new PlayerId("player1")).round(1).build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getBiddingStarted()).isNotNull();
                assertThat(actual.getBiddingStarted().getRound()).isEqualTo(1);
                assertThat(actual.getBiddingStarted().getDealerId()).isEqualTo("player1");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getBiddingStarted()).isNotNull();
                assertThat(actual.getBiddingStarted().getRound()).isEqualTo(1);
                assertThat(actual.getBiddingStarted().getDealerId()).isEqualTo("player1");
            });
        }
        {
            final var event = GameEvent.APlayerBidDeclared.builder()
                    .playerId(new PlayerId("player1")).bidDeclared(1).build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerBidDeclared()).isNotNull();
                assertThat(actual.getAPlayerBidDeclared().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getAPlayerBidDeclared().getBidDeclared()).isEqualTo(1);
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerBidDeclared()).isNotNull();
                assertThat(actual.getAPlayerBidDeclared().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getAPlayerBidDeclared().getBidDeclared()).isEqualTo(-1);
            });
        }
        {
            final var event = GameEvent.RoundStarted.builder()
                    .round(1).deck(40)
                    .players(List.of(
                            GameEvent.RoundStarted.JoinedPlayer.builder()
                                    .playerId(new PlayerId("player1"))
                                    .cardIds(List.of(
                                            new CardId("card1"),
                                            new CardId("card2")
                                    )).build(),
                            GameEvent.RoundStarted.JoinedPlayer.builder()
                                    .playerId(new PlayerId("player2"))
                                    .cardIds(List.of(
                                            new CardId("card3"),
                                            new CardId("card4")
                                    )).build()
                    ))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getRoundStarted()).isNotNull();
                assertThat(actual.getRoundStarted().getRound()).isEqualTo(1);
                assertThat(actual.getRoundStarted().getDeck()).isEqualTo(40);
                assertThat(actual.getRoundStarted().getCard(0)).isEqualTo("card1");
                assertThat(actual.getRoundStarted().getCard(1)).isEqualTo("card2");
                assertThat(actual.getRoundStarted().getJoinedPlayers(0).getPlayerId()).isEqualTo("player1");
                assertThat(actual.getRoundStarted().getJoinedPlayers(0).getCard()).isEqualTo(2);
                assertThat(actual.getRoundStarted().getJoinedPlayers(1).getPlayerId()).isEqualTo("player2");
                assertThat(actual.getRoundStarted().getJoinedPlayers(1).getCard()).isEqualTo(2);
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getRoundStarted()).isNotNull();
                assertThat(actual.getRoundStarted().getRound()).isEqualTo(1);
                assertThat(actual.getRoundStarted().getDeck()).isEqualTo(40);
                assertThat(actual.getRoundStarted().getCard(0)).isEqualTo("card3");
                assertThat(actual.getRoundStarted().getCard(1)).isEqualTo("card4");
                assertThat(actual.getRoundStarted().getJoinedPlayers(0).getPlayerId()).isEqualTo("player1");
                assertThat(actual.getRoundStarted().getJoinedPlayers(0).getCard()).isEqualTo(2);
                assertThat(actual.getRoundStarted().getJoinedPlayers(1).getPlayerId()).isEqualTo("player2");
                assertThat(actual.getRoundStarted().getJoinedPlayers(1).getCard()).isEqualTo(2);
            });
        }
        {
            final var player1 = new Player(new PlayerId("player1"));
            player1.setCards(Map.of(new CardId("card1"),
                    new Card.NumberCard(new CardId("card1"), 10, Card.NumberCard.CardColor.GREEN)));
            player1.setDeclaredBid(3);
            final var player2 = new Player(new PlayerId("player2"));
            player2.setCards(Map.of(new CardId("card2"),
                    new Card.NumberCard(new CardId("card2"), 11, Card.NumberCard.CardColor.BLACK)));
            player2.setDeclaredBid(2);

            final var event = GameEvent.TrickStarted.builder()
                    .trick(5).deck(40)
                    .players(List.of(player1, player2))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getTrickStarted()).isNotNull();
                assertThat(actual.getTrickStarted().getTrick()).isEqualTo(5);
                assertThat(actual.getTrickStarted().getBidPlayers(0).getBid()).isEqualTo(3);
                assertThat(actual.getTrickStarted().getBidPlayers(0).getCard()).isEqualTo(1);
                assertThat(actual.getTrickStarted().getBidPlayers(1).getBid()).isEqualTo(2);
                assertThat(actual.getTrickStarted().getBidPlayers(1).getCard()).isEqualTo(1);
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getTrickStarted()).isNotNull();
                assertThat(actual.getTrickStarted().getTrick()).isEqualTo(5);
                assertThat(actual.getTrickStarted().getBidPlayers(0).getBid()).isEqualTo(3);
                assertThat(actual.getTrickStarted().getBidPlayers(0).getCard()).isEqualTo(1);
                assertThat(actual.getTrickStarted().getBidPlayers(1).getBid()).isEqualTo(2);
                assertThat(actual.getTrickStarted().getBidPlayers(1).getCard()).isEqualTo(1);
            });
        }
        {
            final var event = GameEvent.APlayerTrickPlayed.builder()
                    .playerId(new PlayerId("player1"))
                    .playedCard(new Card.NumberCard(new CardId("card1"), 10, Card.NumberCard.CardColor.GREEN))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerTrickPlayed()).isNotNull();
                assertThat(actual.getAPlayerTrickPlayed().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getCardId()).isEqualTo("card1");
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getNumberCard()).isNotNull();
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getNumberCard().getNumber()).isEqualTo(10);
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getNumberCard().getCardColor())
                        .isEqualTo(gameserver.service.grpc.Card.CardColor.GREEN);
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getAPlayerTrickPlayed()).isNotNull();
                assertThat(actual.getAPlayerTrickPlayed().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getCardId()).isEqualTo("card1");
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getNumberCard()).isNotNull();
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getNumberCard().getNumber()).isEqualTo(10);
                assertThat(actual.getAPlayerTrickPlayed().getPlayedCard().getNumberCard().getCardColor())
                        .isEqualTo(gameserver.service.grpc.Card.CardColor.GREEN);
            });
        }
        {
            final var event = GameEvent.HandChangeAvailableNotice.builder()
                    .playerId(new PlayerId("player1"))
                    .drawCards(List.of(new CardId("card1"), new CardId("card2")))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getHandChangeAvailableNotice()).isNotNull();
                assertThat(actual.getHandChangeAvailableNotice().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getHandChangeAvailableNotice().getDrawCards(0)).isEqualTo("card1");
                assertThat(actual.getHandChangeAvailableNotice().getDrawCards(1)).isEqualTo("card2");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getHandChangeAvailableNotice()).isNotNull();
                assertThat(actual.getHandChangeAvailableNotice().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getHandChangeAvailableNotice().getDrawCardsList()).asList().isEmpty();
            });
        }
        {
            final var event = GameEvent.FuturePredicateAvailable.builder()
                    .playerId(new PlayerId("player1"))
                    .deckCard(List.of(new CardId("card1"), new CardId("card2")))
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getFuturePredicateAvailable()).isNotNull();
                assertThat(actual.getFuturePredicateAvailable().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getFuturePredicateAvailable().getDeckCard(0)).isEqualTo("card1");
                assertThat(actual.getFuturePredicateAvailable().getDeckCard(1)).isEqualTo("card2");
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual.getFuturePredicateAvailable()).isNotNull();
                assertThat(actual.getFuturePredicateAvailable().getPlayerId()).isEqualTo("player1");
                assertThat(actual.getFuturePredicateAvailable().getDeckCardList()).asList().isEmpty();
            });
        }
        {
            final var event = GameEvent.GameException.builder()
                    .playerId(new PlayerId("player1"))
                    .invalidInputType(InputCheckResult.InvalidInputType.DECLARED_INVALID_BID_VALUE)
                    .build();

            assertThat(adapter.adapt(new PlayerId("player1"), event)).satisfies(actual -> {
                assertThat(actual.getGameException()).isNotNull();
                assertThat(actual.getGameException().getInvalidInputType())
                        .isEqualTo(gameserver.service.grpc.GameEvent.InvalidInputType.DECLARED_INVALID_BID_VALUE);
            });

            assertThat(adapter.adapt(new PlayerId("player2"), event)).satisfies(actual -> {
                assertThat(actual).isNull();
            });
        }
    }

}