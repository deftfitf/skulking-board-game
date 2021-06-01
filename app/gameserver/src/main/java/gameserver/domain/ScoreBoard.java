package gameserver.domain;

import lombok.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Value
public class ScoreBoard {
    List<Map<PlayerId, Score>> roundScores;

    public void addRoundScore(Map<PlayerId, Score> playerIdScoreMap) {
        roundScores.add(playerIdScoreMap);
    }

    public static ScoreBoard empty() {
        return new ScoreBoard(new ArrayList<>());
    }

    public Map<PlayerId, Score> getLastRoundScore() {
        return new HashMap<>(roundScores.get(roundScores.size() - 1));
    }
}
