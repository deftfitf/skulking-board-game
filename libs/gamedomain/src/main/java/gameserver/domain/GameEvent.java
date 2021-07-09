package gameserver.domain;

import akka.serialization.jackson.CborSerializable;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "initialized", value = GameEvent.Initialized.class),
        @JsonSubTypes.Type(name = "connection_established", value = GameEvent.ConnectionEstablished.class),
        @JsonSubTypes.Type(name = "connection_closed", value = GameEvent.ConnectionClosed.class),
        @JsonSubTypes.Type(name = "a_player_joined", value = GameEvent.APlayerJoined.class),
        @JsonSubTypes.Type(name = "a_player_left", value = GameEvent.APlayerLeft.class),
        @JsonSubTypes.Type(name = "room_dealer_changed", value = GameEvent.RoomDealerChanged.class),
        @JsonSubTypes.Type(name = "game_started", value = GameEvent.GameStarted.class),
        @JsonSubTypes.Type(name = "bidding_started", value = GameEvent.BiddingStarted.class),
        @JsonSubTypes.Type(name = "a_player_bid_declared", value = GameEvent.APlayerBidDeclared.class),
        @JsonSubTypes.Type(name = "round_started", value = GameEvent.RoundStarted.class),
        @JsonSubTypes.Type(name = "trick_started", value = GameEvent.TrickStarted.class),
        @JsonSubTypes.Type(name = "a_player_trick_played", value = GameEvent.APlayerTrickPlayed.class),
        @JsonSubTypes.Type(name = "a_player_won", value = GameEvent.APlayerWon.class),
        @JsonSubTypes.Type(name = "all_ran_away", value = GameEvent.AllRanAway.class),
        @JsonSubTypes.Type(name = "kraken_appeared", value = GameEvent.KrakenAppeared.class),
        @JsonSubTypes.Type(name = "declare_bid_change_available", value = GameEvent.DeclareBidChangeAvailable.class),
        @JsonSubTypes.Type(name = "next_trick_lead_player_changeable_notice", value = GameEvent.NextTrickLeadPlayerChangeableNotice.class),
        @JsonSubTypes.Type(name = "hand_change_available_notice", value = GameEvent.HandChangeAvailableNotice.class),
        @JsonSubTypes.Type(name = "future_predicate_available", value = GameEvent.FuturePredicateAvailable.class),
        @JsonSubTypes.Type(name = "round_finished", value = GameEvent.RoundFinished.class),
        @JsonSubTypes.Type(name = "next_trick_lead_player_changed", value = GameEvent.NextTrickLeadPlayerChanged.class),
        @JsonSubTypes.Type(name = "player_hand_changed", value = GameEvent.PlayerHandChanged.class),
        @JsonSubTypes.Type(name = "future_predicated", value = GameEvent.FuturePredicated.class),
        @JsonSubTypes.Type(name = "bid_declare_changed", value = GameEvent.BidDeclareChanged.class),
        @JsonSubTypes.Type(name = "game_finished", value = GameEvent.GameFinished.class),
        @JsonSubTypes.Type(name = "game_replayed", value = GameEvent.GameReplayed.class),
        @JsonSubTypes.Type(name = "game_ended", value = GameEvent.GameEnded.class),
        @JsonSubTypes.Type(name = "game_snapshot", value = GameEvent.GameSnapshot.class),
        @JsonSubTypes.Type(name = "game_exception", value = GameEvent.GameException.class),
        @JsonSubTypes.Type(name = "stored", value = GameEvent.Stored.class),
})
public interface GameEvent extends CborSerializable {

    default boolean isPublishOnly() {
        return false;
    }

    @Value
    @Builder
    public static class Initialized implements GameEvent {
        @NonNull String gameRoomId;
        @NonNull PlayerId firstDealerId;
        @NonNull GameRule gameRule;
    }

    @Value
    @Builder
    public static class ConnectionEstablished implements GameEvent {
        @NonNull PlayerId playerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class ConnectionClosed implements GameEvent {
        @NonNull PlayerId playerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class APlayerJoined implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    public static class APlayerLeft implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    public static class RoomDealerChanged implements GameEvent {
        @NonNull PlayerId oldDealer;
        @NonNull PlayerId newDealer;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class GameStarted implements GameEvent {
        @NonNull List<PlayerId> playerIds;
    }

    @Value
    @Builder
    public static class BiddingStarted implements GameEvent {
        int round;
        PlayerId dealerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class APlayerBidDeclared implements GameEvent {
        @NonNull PlayerId playerId;
        int bidDeclared;
    }

    @Value
    @Builder
    public static class RoundStarted implements GameEvent {
        int round;
        int deck;
        @NonNull List<JoinedPlayer> players;

        @Value
        @Builder
        public static class JoinedPlayer {
            PlayerId playerId;
            List<CardId> cardIds;
        }

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class TrickStarted implements GameEvent {
        int deck;
        int trick;
        List<Player> players;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class APlayerTrickPlayed implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull Card playedCard;
    }

    @Value
    @Builder
    public static class APlayerWon implements GameEvent {
        @NonNull PlayerId winnerId;
        @NonNull Card card;
        int trickBonus;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class AllRanAway implements GameEvent {
        @NonNull PlayerId winnerId;
        @NonNull Card card;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class KrakenAppeared implements GameEvent {
        @NonNull PlayerId mustHaveWon;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class DeclareBidChangeAvailable implements GameEvent {
        @NonNull PlayerId playerId;
        int min;
        int max;
    }

    @Value
    @Builder
    public static class NextTrickLeadPlayerChangeableNotice implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    public static class HandChangeAvailableNotice implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull List<CardId> drawCards;
    }

    @Value
    @Builder
    public static class FuturePredicateAvailable implements GameEvent {
        PlayerId playerId;
        List<CardId> deckCard;
    }

    @Value
    @Builder
    public static class RoundFinished implements GameEvent {
        Map<PlayerId, Score> roundScore;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class NextTrickLeadPlayerChanged implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull PlayerId newLeadPlayerId;
    }

    @Value
    @Builder
    public static class PlayerHandChanged implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull Set<CardId> returnCards;
    }

    @Value
    @Builder
    public static class FuturePredicated implements GameEvent {
        @NonNull PlayerId predicatedPlayerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    public static class BidDeclareChanged implements GameEvent {
        @NonNull PlayerId changedPlayerId;
        int changedBid;
    }

    @Value
    @Builder
    public static class GameFinished implements GameEvent {
        @NonNull PlayerId gameWinnerId;
        @NonNull ScoreBoard scoreBoard;
    }

    @Value
    @Builder
    public static class GameReplayed implements GameEvent {
        @NonNull PlayerId gameWinnerId;
    }

    @Value
    @Builder
    public static class GameEnded implements GameEvent {
    }

    @Value
    @Builder
    public static class GameSnapshot implements GameEvent {
        @NonNull String gameRoomId;
        @NonNull GameState gameState;
    }

    @Value
    @Builder
    public static class GameException implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull InputCheckResult.InvalidInputType invalidInputType;
    }

    @Value
    @Builder
    public static class Stored implements GameEvent {
        GameState state;
    }

}
