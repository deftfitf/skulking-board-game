package gameserver.service.impl;


import gameserver.domain.Card;
import gameserver.domain.CardId;

public class CardAdapter {

    public Card adapt(gameserver.service.grpc.Card card) {
        final var cardId = new CardId(card.getCardId());
        switch (card.getCardCase()) {
            case KRAKEN:
                return new Card.Kraken(cardId);
            case MERMAID:
                return new Card.Mermaid(cardId);
            case TIGRESS:
                return new Card.Tigress(cardId, card.getTigress().getIsPirates());
            case SKULKING:
                return new Card.Skulking(cardId);
            case JANITA_JADE:
                return new Card.JuanitaJade(cardId);
            case NUMBER_CARD:
                return new Card.NumberCard(
                        cardId,
                        card.getNumberCard().getNumber(),
                        Card.NumberCard.CardColor.valueOf(card.getNumberCard().getCardColor().name()));
            case STANDARD_ESCAPE:
                return new Card.StandardEscape(cardId);
            case ROISE_D_LANEY:
                return new Card.RoiseDLaney(cardId);
            case HARRY_THE_GIANT:
                return new Card.HarryTheGiant(cardId);
            case RASCAL_OF_ROATAN:
                return new Card.RascalOfRoatan(cardId, card.getRascalOfRoatan().getBetScore());
            case STANDARD_PIRATES:
                return new Card.StandardPirates(cardId);
            case BAHIJI_THE_BANDIT:
                return new Card.BahijTheBandit(cardId);
            default:
                throw new IllegalArgumentException("not supported card detected");
        }
    }

    public gameserver.service.grpc.Card adapt(Card card) {
        final var bldr = gameserver.service.grpc.Card.newBuilder();

        bldr.setCardId(card.getCardId().getId());
        if (card instanceof Card.NumberCard) {
            final var numberCard = (Card.NumberCard) card;
            bldr.setNumberCard(gameserver.service.grpc.Card.NumberCard.newBuilder()
                    .setNumber(numberCard.getNumber())
                    .setCardColor(gameserver.service.grpc.Card.CardColor.valueOf(numberCard.getCardColor().name()))
                    .build());
        } else if (card instanceof Card.StandardPirates) {
            bldr.setStandardPirates(gameserver.service.grpc.Card.StandardPirates.newBuilder().build());
        } else if (card instanceof Card.RoiseDLaney) {
            bldr.setRoiseDLaney(gameserver.service.grpc.Card.RoiseDLaney.newBuilder().build());
        } else if (card instanceof Card.BahijTheBandit) {
            bldr.setBahijiTheBandit(gameserver.service.grpc.Card.BahijiTheBandit.newBuilder().build());
        } else if (card instanceof Card.RascalOfRoatan) {
            final var rascal = (Card.RascalOfRoatan) card;
            bldr.setRascalOfRoatan(gameserver.service.grpc.Card.RascalOfRoatan.newBuilder()
                    .setBetScore(rascal.getBetScore())
                    .build());
        } else if (card instanceof Card.JuanitaJade) {
            bldr.setJanitaJade(gameserver.service.grpc.Card.JanitaJade.newBuilder().build());
        } else if (card instanceof Card.HarryTheGiant) {
            bldr.setHarryTheGiant(gameserver.service.grpc.Card.HarryTheGiant.newBuilder().build());
        } else if (card instanceof Card.StandardEscape) {
            bldr.setStandardEscape(gameserver.service.grpc.Card.StandardEscape.newBuilder().build());
        } else if (card instanceof Card.Tigress) {
            final var tigress = (Card.Tigress) card;
            bldr.setTigress(gameserver.service.grpc.Card.Tigress.newBuilder()
                    .setIsPirates(tigress.getIsPirates())
                    .build());
        } else if (card instanceof Card.Skulking) {
            bldr.setSkulking(gameserver.service.grpc.Card.Skulking.newBuilder().build());
        } else if (card instanceof Card.Mermaid) {
            bldr.setMermaid(gameserver.service.grpc.Card.Mermaid.newBuilder().build());
        } else if (card instanceof Card.Kraken) {
            bldr.setKraken(gameserver.service.grpc.Card.Kraken.newBuilder().build());
        } else {
            throw new IllegalArgumentException("illegal card detected");
        }

        return bldr.build();
    }

}
