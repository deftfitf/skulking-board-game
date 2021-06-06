package gameserver.domain;

import lombok.Value;

import java.util.LinkedList;

@Value
public class GameRule {
    public static final int ROOM_MIN_SIZE = 2;
    public static final int ROOM_MAX_MAX_SIZE = 6;

    int roomSize;
    int nOfRounds;
    DeckType deckType;

    public enum DeckType {
        STANDARD,
        EXPANSION,
        ;
    }

    public LinkedList<Card> provideNewDeck() {
        if (deckType == DeckType.STANDARD) {
            return CardDeckProvider.standardDeck();
        }
        return CardDeckProvider.expansionDeck();
    }

}