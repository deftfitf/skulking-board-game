package gameserver.domain;

import java.util.LinkedList;

public class CardDeckProvider {

    public static LinkedList<Card> standardDeck() {
        final var deck = new LinkedList<Card>();

        deck.addAll(Card.NumberCard.allCards());
        deck.addAll(Card.StandardEscape.standardEscapes(5));
        deck.addAll(Card.StandardPirates.standardPirates(5));
        deck.add(Card.Tigress.newInstance());
        deck.add(Card.Skulking.newInstance());

        return deck;
    }

    public static LinkedList<Card> expansionDeck() {
        final var deck = new LinkedList<Card>();

        deck.addAll(Card.NumberCard.allCards());
        deck.addAll(Card.StandardEscape.standardEscapes(5));
        deck.addAll(Card.Mermaid.mermaidCards(2));
        deck.add(Card.RoiseDLaney.newInstance());
        deck.add(Card.BahijTheBandit.newInstance());
        deck.add(Card.RascalOfRoatan.newInstance());
        deck.add(Card.JuanitaJade.newInstance());
        deck.add(Card.HarryTheGiant.newInstance());
        deck.add(Card.Tigress.newInstance());
        deck.add(Card.Skulking.newInstance());
        deck.add(Card.Kraken.newInstance());

        return deck;
    }

}
