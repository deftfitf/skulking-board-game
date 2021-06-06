package gameserver.service.impl;

import gameserver.domain.PlayerId;
import gameserver.domain.ScoreBoard;
import gameserver.service.grpc.Score;

import java.util.Map;
import java.util.stream.Collectors;

public class ScoreBoardAdapter {

    public gameserver.service.grpc.ScoreBoard adapt(ScoreBoard _scoreBoard) {
        final var roundScores = _scoreBoard.getRoundScores()
                .stream().map(this::adapt)
                .map(roundScore -> gameserver.service.grpc.ScoreBoard.RoundScore.newBuilder().putAllRoundScore(roundScore).build())
                .collect(Collectors.toList());
        return gameserver.service.grpc.ScoreBoard.newBuilder()
                .addAllRoundScores(roundScores)
                .build();
    }

    public Map<String, Score> adapt(Map<PlayerId, gameserver.domain.Score> _score) {
        return _score.entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getValue(),
                        entry -> gameserver.service.grpc.Score.newBuilder()
                                .setScore(entry.getValue().getScore())
                                .setBonus(entry.getValue().getBonus())
                                .build()));
    }

}
