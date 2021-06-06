package gameserver.domain;

import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

public interface InputCheckResult {

    public enum ApplyableInput implements InputCheckResult {
        INSTANCE
    }

    @Value
    @Builder
    public class InvalidInput implements InputCheckResult {
        @NonNull InvalidInputType invalidInputType;
    }

    public enum InvalidInputType {
        FAILED_JOIN_EXCEED_MAX_NUMBER_OF_PLAYERS,
        FAILED_JOIN_ALREADY_JOINED_PLAYER,
        FAILED_LEAVE_SPECIFIED_PLAYER_NOT_EXISTS,
        FAILED_START_INSUFFICIENT_PLAYERS,
        FAILED_START_GAME_NOT_DEALER,

        ROUND_HAS_ALREADY_ENDED,
        ROUND_HAS_NOT_ENDED_YET,
        IS_NOT_NEXT_PLAYER,
        HAS_NOT_CARD,
        CANT_PUT_CARD_ON_FIELD,

        INVALID_CHANGE_BID_VALUE,
        ILLEGAL_PLAYER_ACTION_DETECTED,

        SPECIFIED_PLAYER_ID_DOES_NOT_EXISTS,
        DECLARED_INVALID_BID_VALUE,

        RETURN_CARD_SIZE_INVALID,
        RETURN_CARD_PLAYER_NOT_HAS,
        ;
    }

}