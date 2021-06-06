package gameserver.service.impl;


import gameserver.domain.GameRule;

public class GameRuleAdapter {

    public gameserver.service.grpc.GameRule adapt(GameRule _gameRule) {
        final var bldr = gameserver.service.grpc.GameRule.newBuilder();

        switch (_gameRule.getDeckType()) {
            case STANDARD:
                bldr.setDeckType(gameserver.service.grpc.GameRule.DeckType.STANDARD);
                break;
            case EXPANSION:
                bldr.setDeckType(gameserver.service.grpc.GameRule.DeckType.EXPANSION);
                break;
            default:
                throw new IllegalArgumentException("illegal deck type specified");
        }

        bldr.setNOfRounds(_gameRule.getNOfRounds());
        bldr.setRoomSize(_gameRule.getRoomSize());

        return bldr.build();
    }

    public GameRule adapt(gameserver.service.grpc.GameRule _gameRule) {
        final var bldr = GameRule.builder();

        switch (_gameRule.getDeckType()) {
            case STANDARD:
                bldr.deckType(GameRule.DeckType.STANDARD);
                break;
            case EXPANSION:
                bldr.deckType(GameRule.DeckType.EXPANSION);
                break;
            default:
                throw new IllegalArgumentException("illegal deck type sepcified");
        }

        bldr.roomSize(_gameRule.getRoomSize());
        bldr.nOfRounds(_gameRule.getNOfRounds());

        return bldr.build();
    }

}
