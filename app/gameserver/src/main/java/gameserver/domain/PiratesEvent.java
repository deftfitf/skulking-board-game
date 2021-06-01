package gameserver.domain;

import lombok.Builder;
import lombok.Value;

import java.util.List;

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