package gameserver.domain;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "next_trick_lead_player_changeable_notice", value = PiratesEvent.NextTrickLeadPlayerChangeableNotice.class),
        @JsonSubTypes.Type(name = "hand_change_available_notice", value = PiratesEvent.HandChangeAvailableNotice.class),
        @JsonSubTypes.Type(name = "got_bonus_score", value = PiratesEvent.GotBonusScore.class),
        @JsonSubTypes.Type(name = "future_predicate_available", value = PiratesEvent.FuturePredicateAvailable.class),
        @JsonSubTypes.Type(name = "declare_bid_change_available", value = PiratesEvent.DeclareBidChangeAvailable.class),
})
public interface PiratesEvent {

    @Value
    @Builder
    class NextTrickLeadPlayerChangeableNotice implements PiratesEvent {
        PlayerId playerId;
    }

    @Value
    @Builder
    class HandChangeAvailableNotice implements PiratesEvent {
        PlayerId playerId;
        List<CardId> drawCards;
    }

    @Value
    @Builder
    class GotBonusScore implements PiratesEvent {
        PlayerId playerId;
        int bonusScore;
    }

    @Value
    @Builder
    class FuturePredicateAvailable implements PiratesEvent {
        PlayerId playerId;
    }

    @Value
    @Builder
    class DeclareBidChangeAvailable implements PiratesEvent {
        PlayerId playerId;
        int min;
        int max;
    }

}