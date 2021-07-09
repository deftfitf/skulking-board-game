package gameserver.domain;

import akka.actor.typed.ActorRef;
import akka.serialization.jackson.CborSerializable;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.util.Set;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "init", value = GameCommand.Init.class),
        @JsonSubTypes.Type(name = "ping", value = GameCommand.Ping.class),
        @JsonSubTypes.Type(name = "new_connection", value = GameCommand.NewConnection.class),
        @JsonSubTypes.Type(name = "close_connection", value = GameCommand.CloseConnection.class),
        @JsonSubTypes.Type(name = "join", value = GameCommand.Join.class),
        @JsonSubTypes.Type(name = "leave", value = GameCommand.Leave.class),
        @JsonSubTypes.Type(name = "game_start", value = GameCommand.GameStart.class),
        @JsonSubTypes.Type(name = "bid_declare", value = GameCommand.BidDeclare.class),
        @JsonSubTypes.Type(name = "play_card", value = GameCommand.PlayCard.class),
        @JsonSubTypes.Type(name = "next_trick_lead_player_change", value = GameCommand.NextTrickLeadPlayerChange.class),
        @JsonSubTypes.Type(name = "player_hand_change", value = GameCommand.PlayerHandChange.class),
        @JsonSubTypes.Type(name = "future_predicate_finish", value = GameCommand.FuturePredicateFinish.class),
        @JsonSubTypes.Type(name = "bid_declare_change", value = GameCommand.BidDeclareChange.class),
        @JsonSubTypes.Type(name = "replay_game", value = GameCommand.ReplayGame.class),
        @JsonSubTypes.Type(name = "end_game", value = GameCommand.EndGame.class),
        @JsonSubTypes.Type(name = "snapshot_request", value = GameCommand.SnapshotRequest.class),
        @JsonSubTypes.Type(name = "store", value = GameCommand.Store.class),
})
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

