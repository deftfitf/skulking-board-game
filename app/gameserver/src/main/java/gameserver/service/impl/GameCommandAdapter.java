package gameserver.service.impl;

import gameserver.domain.CardId;
import gameserver.domain.GameCommand;
import gameserver.domain.PlayerId;
import lombok.RequiredArgsConstructor;

import java.util.stream.Collectors;

@RequiredArgsConstructor
public class GameCommandAdapter {
    private final CardAdapter cardAdapter;

    public GameCommand adapt(gameserver.service.grpc.GameCommand _gameCommand) {
        final var playerId = new PlayerId(_gameCommand.getPlayerId());
        switch (_gameCommand.getCmdCase()) {
            case JOIN:
                return GameCommand.Join.builder()
                        .playerId(playerId)
                        .build();
            case LEAVE:
                return GameCommand.Leave.builder()
                        .playerId(playerId)
                        .build();
            case END_GAME:
                return GameCommand.EndGame.builder()
                        .playerId(playerId)
                        .build();
            case GAME_START:
                return GameCommand.GameStart.builder()
                        .playerId(playerId)
                        .build();
            case PLAY_CARD:
                final var card = cardAdapter.adapt(_gameCommand.getPlayCard().getCard());
                return GameCommand.PlayCard.builder()
                        .playerId(playerId)
                        .card(card)
                        .build();
            case BID_DECLARE:
                return GameCommand.BidDeclare.builder()
                        .playerId(playerId)
                        .bid(_gameCommand.getBidDeclare().getBid())
                        .build();
            case REPLAY_GAME:
                return GameCommand.ReplayGame.builder()
                        .playerId(playerId)
                        .build();
            case SNAPSHOT_REQUEST:
                return GameCommand.SnapshotRequest.builder()
                        .playerId(playerId)
                        .build();
            case BID_DECLARE_CHANGE:
                return GameCommand.BidDeclareChange.builder()
                        .playerId(playerId)
                        .bid(_gameCommand.getBidDeclare().getBid())
                        .build();
            case PLAYER_HAND_CHANGE:
                final var returnCards = _gameCommand.getPlayerHandChange()
                        .getCardIdList().stream()
                        .map(CardId::new)
                        .collect(Collectors.toSet());
                return GameCommand.PlayerHandChange.builder()
                        .playerId(playerId)
                        .returnCards(returnCards)
                        .build();
            case FUTURE_PREDICATE_FINISH:
                return GameCommand.FuturePredicateFinish.builder()
                        .predicatePlayerId(playerId)
                        .build();
            case NEXT_TRICK_LEAD_PLAYER_CHANGE:
                final var newLeadPlayerId = new PlayerId(_gameCommand.getNextTrickLeadPlayerChange().getNewLeadPlayerId());
                return GameCommand.NextTrickLeadPlayerChange.builder()
                        .playerId(playerId)
                        .newLeadPlayerId(newLeadPlayerId)
                        .build();
            default:
                throw new UnsupportedOperationException("unsupported message was sent");
        }
    }

}
