package gameserver.domain;

import akka.serialization.jackson.CborSerializable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

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
