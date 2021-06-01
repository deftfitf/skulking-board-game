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
    class Initialized implements GameEvent {
        @NonNull PlayerId firstDealerId;
        @NonNull GameRule gameRule;
    }

    @Value
    @Builder
    class ConnectionEstablished implements GameEvent {
        @NonNull PlayerId playerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class ConnectionClosed implements GameEvent {
        @NonNull PlayerId playerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class APlayerJoined implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    class APlayerLeft implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    class RoomDealerChanged implements GameEvent {
        @NonNull PlayerId oldDealer;
        @NonNull PlayerId newDealer;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class GameStarted implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    class BiddingStarted implements GameEvent {
        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class APlayerBidDeclared implements GameEvent {
        @NonNull PlayerId playerId;
        int bidDeclared;
    }

    @Value
    @Builder
    class RoundStarted implements GameEvent {
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
    class TrickStarted implements GameEvent {
        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class APlayerTrickPlayed implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull Card playedCard;
    }

    @Value
    @Builder
    class APlayerWon implements GameEvent {
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
    class AllRanAway implements GameEvent {
        @NonNull PlayerId winnerId;
        @NonNull Card card;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class KrakenAppeared implements GameEvent {
        @NonNull PlayerId mustHaveWon;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class DeclareBidChangeAvailable implements GameEvent {
        @NonNull PlayerId playerId;
        int min;
        int max;
    }

    @Value
    @Builder
    class NextTrickLeadPlayerChangeableNotice implements GameEvent {
        @NonNull PlayerId playerId;
    }

    @Value
    @Builder
    class HandChangeAvailableNotice implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull List<CardId> drawCards;
    }

    @Value
    @Builder
    class FuturePredicateAvailable implements GameEvent {
        PlayerId playerId;
        List<CardId> deckCard;
    }

    @Value
    @Builder
    class RoundFinished implements GameEvent {
        Map<PlayerId, Score> roundScore;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class NextTrickLeadPlayerChanged implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull PlayerId newLeadPlayerId;
    }

    @Value
    @Builder
    class PlayerHandChanged implements GameEvent {
        @NonNull PlayerId playerId;
        @NonNull Set<CardId> returnCards;
    }

    @Value
    @Builder
    class FuturePredicated implements GameEvent {
        @NonNull PlayerId predicatedPlayerId;

        @Override
        public boolean isPublishOnly() {
            return true;
        }
    }

    @Value
    @Builder
    class BidDeclareChanged implements GameEvent {
        @NonNull PlayerId changedPlayerId;
        int changedBid;
    }

    @Value
    @Builder
    class BulkEvent implements GameEvent {
        List<GameEvent> gameEventList;
    }

    @Value
    @Builder
    class GameFinished implements GameEvent {
        @NonNull PlayerId gameWinnerId;
        @NonNull ScoreBoard scoreBoard;
    }

    @Value
    @Builder
    class GameReplayed implements GameEvent {
        @NonNull PlayerId gameWinnerId;
    }

    @Value
    @Builder
    class GameEnded implements GameEvent {
    }

    @Value
    @Builder
    class GameSnapshot implements GameEvent {
        @NonNull GameState gameState;
    }

    @Value
    @Builder
    class GameException implements GameEvent {
        @NonNull InputCheckResult.InvalidInputType invalidInputType;
    }

    @Value
    @Builder
    class Stored implements GameEvent {
        GameState state;
    }

}
