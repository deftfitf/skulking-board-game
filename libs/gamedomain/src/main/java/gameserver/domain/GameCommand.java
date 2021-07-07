package gameserver.domain;

import akka.actor.typed.ActorRef;
import akka.serialization.jackson.CborSerializable;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

public interface GameCommand extends CborSerializable {

    public PlayerId callerId();

    @Value
    @Builder
    class Init implements GameCommand {
        @NonNull PlayerId firstDealerId;
        @NonNull GameRule gameRule;
        @NonNull ActorRef<GameEvent.Initialized> response;

        @Override
        public PlayerId callerId() {
            return firstDealerId;
        }
    }

    @Value
    @Builder
    class Ping implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull ActorRef<GameEvent> playerRef;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class NewConnection implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull ActorRef<GameEvent> playerRef;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class CloseConnection implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull ActorRef<GameEvent> playerRef;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class Join implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull ActorRef<GameEvent> playerRef;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class Leave implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull ActorRef<GameEvent> playerRef;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class GameStart implements GameCommand {
        @NonNull PlayerId playerId;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class BidDeclare implements GameCommand {
        @NonNull PlayerId playerId;
        int bid;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class PlayCard implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull Card card;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class NextTrickLeadPlayerChange implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull PlayerId newLeadPlayerId;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class PlayerHandChange implements GameCommand {
        @NonNull PlayerId playerId;
        @NonNull Set<CardId> returnCards;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class FuturePredicateFinish implements GameCommand {
        @NonNull PlayerId predicatePlayerId;

        @Override
        public PlayerId callerId() {
            return predicatePlayerId;
        }
    }

    @Value
    @Builder
    class BidDeclareChange implements GameCommand {
        @NonNull PlayerId playerId;
        int bid;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class ReplayGame implements GameCommand {
        @NonNull PlayerId playerId;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class EndGame implements GameCommand {
        @NonNull PlayerId playerId;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class SnapshotRequest implements GameCommand {
        @NonNull PlayerId playerId;

        @Override
        public PlayerId callerId() {
            return playerId;
        }
    }

    @Value
    @Builder
    class Store implements GameCommand {
        GameState state;

        @Override
        public PlayerId callerId() {
            return null;
        }
    }

}

