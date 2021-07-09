package gameserver.domain;

import akka.serialization.jackson.CborSerializable;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Value;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
@JsonSubTypes({
        @JsonSubTypes.Type(name = "number_card", value = Card.NumberCard.class),
        @JsonSubTypes.Type(name = "standard_pirates", value = Card.StandardPirates.class),
        @JsonSubTypes.Type(name = "roise_d_laney", value = Card.RoiseDLaney.class),
        @JsonSubTypes.Type(name = "bahij_the_bandit", value = Card.BahijTheBandit.class),
        @JsonSubTypes.Type(name = "rascal_of_roatan", value = Card.RascalOfRoatan.class),
        @JsonSubTypes.Type(name = "juanita_jade", value = Card.JuanitaJade.class),
        @JsonSubTypes.Type(name = "harry_the_giant", value = Card.HarryTheGiant.class),
        @JsonSubTypes.Type(name = "standard_escape", value = Card.StandardEscape.class),
        @JsonSubTypes.Type(name = "tigress", value = Card.Tigress.class),
        @JsonSubTypes.Type(name = "skulking", value = Card.Skulking.class),
        @JsonSubTypes.Type(name = "mermaid", value = Card.Mermaid.class),
        @JsonSubTypes.Type(name = "kraken", value = Card.Kraken.class),
})
public interface Card extends CborSerializable {
    CardId getCardId();

    boolean battle(Card next);

    @Value
    class NumberCard implements Card {
        private static final int MAX_CARD_NUMBER = 14;
        private static final int MIN_CARD_NUMBER = 1;

        CardId cardId;
        int number;
        CardColor cardColor;

        @Override
        public boolean battle(Card next) {
            if (next instanceof NumberCard) {
                final var numberCard = (NumberCard) next;

                if (cardColor == numberCard.getCardColor()) {
                    return number > numberCard.number;
                } else if (cardColor != CardColor.BLACK && numberCard.cardColor == CardColor.BLACK) {
                    return false;
                }

                return true;
            } else if (next instanceof Tigress) {
                final var tigressCard = (Tigress) next;
                return !tigressCard.getIsPirates();
            } else if (next instanceof Escape) {
                return true;
            } else {

                return false;
            }
        }

        public enum CardColor {
            GREEN,
            YELLOW,
            PURPLE,
            BLACK,
            ;
        }

        public int getBonusPoint() {
            if (number != MAX_CARD_NUMBER) {
                return 0;
            }

            if (cardColor == CardColor.BLACK) {
                return 20;
            }
            return 10;
        }

        public static List<NumberCard> allCards() {
            return Stream.of(CardColor.values())
                    .flatMap(color -> Stream.iterate(MIN_CARD_NUMBER, n -> n <= MAX_CARD_NUMBER, n -> n + 1)
                            .map(n -> new NumberCard(
                                    new CardId("number:" + color + ':' + n),
                                    n, color
                            )))
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    interface Pirates extends Card {

        default boolean battle(Card next) {
            return !(next instanceof Skulking) && !(next instanceof Kraken);
        }

    }

    @Value
    class StandardPirates implements Pirates {
        CardId cardId;

        public static List<StandardEscape> standardPirates(int n) {
            return Stream
                    .iterate(0, i -> i < n, i -> i + 1)
                    .map(i -> new StandardEscape(
                            new CardId("pirates:" + i)
                    ))
                    .collect(Collectors.toUnmodifiableList());
        }

    }

    @Value
    class RoiseDLaney implements Pirates {
        CardId cardId;

        public static RoiseDLaney newInstance() {
            return new RoiseDLaney(new CardId("roiseDLaney"));
        }

    }

    @Value
    class BahijTheBandit implements Pirates {
        CardId cardId;

        public static BahijTheBandit newInstance() {
            return new BahijTheBandit(new CardId("bahijTheBandit"));
        }
    }

    @Value
    class RascalOfRoatan implements Pirates {
        CardId cardId;
        Integer betScore;

        public static RascalOfRoatan newInstance() {
            return new RascalOfRoatan(
                    new CardId("rascalOfRoatan"),
                    null);
        }

    }

    @Value
    class JuanitaJade implements Pirates {
        CardId cardId;

        public static JuanitaJade newInstance() {
            return new JuanitaJade(new CardId("juanitaJade"));
        }
    }

    @Value
    class HarryTheGiant implements Pirates {
        CardId cardId;

        public static HarryTheGiant newInstance() {
            return new HarryTheGiant(new CardId("harryTheGiant"));
        }
    }

    interface Escape extends Card {
    }

    @Value
    class StandardEscape implements Escape {
        CardId cardId;

        public static List<StandardEscape> standardEscapes(int n) {
            return Stream
                    .iterate(0, i -> i < n, i -> i + 1)
                    .map(i -> new StandardEscape(
                            new CardId("escape:" + i)
                    ))
                    .collect(Collectors.toUnmodifiableList());
        }

        @Override
        public boolean battle(Card next) {
            if (next instanceof Escape) {
                return true;
            } else if (next instanceof Tigress) {
                final var tigressCard = (Tigress) next;
                return !tigressCard.getIsPirates();
            }
            return false;
        }
    }

    @Value
    class Tigress implements Card {
        CardId cardId;
        Boolean isPirates;

        public static Tigress newInstance() {
            return new Tigress(new CardId("tigress"), null);
        }

        @Override
        public boolean battle(Card next) {
            if (isPirates) {
                return !(next instanceof Skulking) && !(next instanceof Kraken);
            } else {
                if (next instanceof Escape) {
                    return true;
                } else if (next instanceof Tigress) {
                    final var tigressCard = (Tigress) next;
                    return !tigressCard.getIsPirates();
                }
                return false;
            }
        }

        public Tigress withIsPirates(boolean isPirates) {
            return new Tigress(cardId, isPirates);
        }
    }

    @Value
    class Skulking implements Card {
        CardId cardId;

        public static Skulking newInstance() {
            return new Skulking(new CardId("skulking"));
        }

        @Override
        public boolean battle(Card next) {
            return !(next instanceof Mermaid) && !(next instanceof Kraken);
        }
    }

    @Value
    class Mermaid implements Card {
        CardId cardId;

        public static List<Mermaid> mermaidCards(int n) {
            return Stream
                    .iterate(0, i -> i < n, i -> i + 1)
                    .map(i -> new Mermaid(new CardId("mermaid:" + i)))
                    .collect(Collectors.toUnmodifiableList());
        }

        @Override
        public boolean battle(Card next) {
            return !(next instanceof Skulking)
                    && !(next instanceof Pirates)
                    && !(next instanceof Kraken);
        }
    }

    @Value
    class Kraken implements Card {
        CardId cardId;

        public static Kraken newInstance() {
            return new Kraken(new CardId("kraken"));
        }

        @Override
        public boolean battle(Card next) {
            return true;
        }
    }

}

