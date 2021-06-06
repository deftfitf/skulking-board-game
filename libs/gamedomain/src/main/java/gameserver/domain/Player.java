package gameserver.domain;

import lombok.Data;

import java.util.Map;

@Data
public class Player {
    final PlayerId playerId;
    Integer declaredBid;
    int tookTrick;
    Map<CardId, Card> cards;
    int tookBonus;

    public boolean hasCard(CardId cardId) {
        return cards.containsKey(cardId);
    }

    public boolean hasColorCard(Card.NumberCard.CardColor cardColor) {
        return cards.values().stream().anyMatch(card -> {
            if (card instanceof Card.NumberCard) {
                return ((Card.NumberCard) card).getCardColor() == cardColor;
            }
            return false;
        });
    }

    public void removeCard(CardId cardId) {
        cards.remove(cardId);
    }

    public void gotATrick() {
        tookTrick++;
    }

    public void addTookBonus(int tookBonus) {
        this.tookBonus += tookBonus;
    }

    public Score getRoundScore(int round) {
        final int baseScore;
        if (declaredBid == tookTrick) {
            if (declaredBid == 0) {
                baseScore = round * 10;
            } else {
                baseScore = declaredBid * 20;
            }
            return new Score(baseScore, tookBonus);
        } else {
            if (declaredBid == 0) {
                baseScore = -round * 10;
            } else {
                baseScore = -Math.abs(declaredBid - tookTrick) * 10;
            }
            return new Score(baseScore, 0);
        }
    }
}
