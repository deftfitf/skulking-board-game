import {
  GameRule as ProtoGameRule,
  GameState as ProtoGameState,
  ScoreBoard as ProtoScoreBoard
} from "../proto/GameServerService_pb";
import {
  BiddingPhase,
  DeckType,
  GameEventStack,
  GameRule,
  GameState,
  ScoreBoard,
  StartPhase,
  WaitForInitialize
} from "./GameModels";

export const deckTypeAdapter = (deckType: ProtoGameRule.DeckType): DeckType => {
  switch (deckType) {
    case ProtoGameRule.DeckType.STANDARD:
      return DeckType.STANDARD;
    case ProtoGameRule.DeckType.EXPANSION:
      return DeckType.EXPANSION;
    default:
      throw new Error("invalid deck type detected");
  }
}

export const gameRuleAdapter = (gameRule: ProtoGameRule): GameRule => {
  return {
    roomSize: gameRule.getRoomSize(),
    nOfRounds: gameRule.getNOfRounds(),
    deckType: deckTypeAdapter(gameRule.getDeckType())
  }
}

export const scoreBoardAdapter = (scoreBoard: ProtoScoreBoard): ScoreBoard => {
  const gameScore: ScoreBoard = [];

  scoreBoard.getRoundScoresList()
  .forEach(roundScore => {
    const roundScoreMap = new Map<string, { score: number, bonus: number }>();
    roundScore.getRoundScoreMap().forEach((score, key) => {
      roundScoreMap.set(key, {
        score: score.getScore(),
        bonus: score.getBonus(),
      });
    });
    gameScore.push(roundScoreMap);
  });

  return gameScore;
}

export const snapshotToGameState = (myPlayerId: string, state: ProtoGameState): GameState => {
  const roomOwnerId = state.getRoomOwnerId();
  switch (state.getStateCase()) {
    case ProtoGameState.StateCase.START_PHASE:
      const startPhase = state.getStartPhase()!;
      startPhase.getDealerId()

      return new StartPhase(
      myPlayerId,
      new GameEventStack(),
      gameRuleAdapter(startPhase.getGameRule()!),
      roomOwnerId,
      startPhase.getPlayerIdsList()
      );

    case ProtoGameState.StateCase.BIDDING_PHASE:
      const biddingPhase = state.getBiddingPhase()!;

      return new BiddingPhase(
      myPlayerId,
      new GameEventStack(),
      gameRuleAdapter(biddingPhase.getGameRule()!),
      roomOwnerId,
      biddingPhase.getDealerId(),
      biddingPhase.getDeck(),
      biddingPhase.getBiddingPlayersList().map(player => ({
        playerId: player.getPlayerId(),
        isBid: player.getIsBid(),
        card: player.getCard(),
      })),
      biddingPhase.getCardList(),
      biddingPhase.getMybid() >= 0 ? biddingPhase.getMybid() : null,
      biddingPhase.getRound(),
      scoreBoardAdapter(biddingPhase.getScoreBoard()!)
      );

    case ProtoGameState.StateCase.TRICK_PHASE:
      

    case ProtoGameState.StateCase.NEXT_TRICK_LEAD_PLAYER_CHANGING:


    case ProtoGameState.StateCase.HAND_CHANGE_WAITING:


    case ProtoGameState.StateCase.FUTURE_PREDICATE_WAITING:


    case ProtoGameState.StateCase.BID_DECLARE_CHANGE_WAITING:


    case ProtoGameState.StateCase.FINISHED_PHASE:


    default:
      throw new Error("illegal state detected");
  }

  return new WaitForInitialize(() => {
  });
}