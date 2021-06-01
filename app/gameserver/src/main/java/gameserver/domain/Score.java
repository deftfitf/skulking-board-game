package gameserver.domain;

import lombok.Value;

@Value
public class Score {
    int score;
    int bonus;

    public int getAll() {
        return score + bonus;
    }
}
