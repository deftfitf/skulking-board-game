import {
  Card,
  GameRule as ProtoGameRule,
  GameState as ProtoGameState,
  ScoreBoard as ProtoScoreBoard
} from "../proto/GameServerService_pb";
import {
  BidDeclareChangeWaitingPhase,
  BiddingPhase,
  DeckType,
  FinishedPhase,
  FuturePredicateWaitingPhase,
  GameEventStack,
  GameRule,
  GameState,
  HandChangeWaitingPhase,
  MustFollow,
  NextTrickLeadPlayerChangingPhase,
  ScoreBoard,
  StartPhase,
  TrickPhase
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

export const mustFollowAdapter = (cardColor: Card.CardColor): MustFollow | null => {
  switch (cardColor) {
    case Card.CardColor.UNKNOWN:
      return null;
    case Card.CardColor.BLACK:
      return MustFollow.BLACK;
    case Card.CardColor.GREEN:
      return MustFollow.GREEN;
    case Card.CardColor.PURPLE:
      return MustFollow.PURPLE;
    case Card.CardColor.YELLOW:
      return MustFollow.YELLOW;
    default:
      throw new Error("unexpected card color detected");
  }
}

export const trickPhaseAdapter = (
gameRoomId: string,
myPlayerId: string,
roomOwnerId: string,
deck: Map<string, Card>,
trickPhase: ProtoGameState.TrickPhase
): TrickPhase => {
  return new TrickPhase(
  deck,
  gameRoomId,
  myPlayerId,
  new GameEventStack(),
  gameRuleAdapter(trickPhase.getGameRule()!),
  roomOwnerId,
  trickPhase.getRound(),
  trickPhase.getDealerId(),
  trickPhase.getNextPlayerId(),
  trickPhase.getTrickPlayersList().map(player => ({
    playerId: player.getPlayerId(),
    declaredBid: player.getDeclaredBid(),
    tookTrick: player.getTookTrick(),
    card: player.getCard(),
    tookBonus: player.getTookBonus()
  })),
  trickPhase.getCardList(),
  trickPhase.getDeck(),
  trickPhase.getStack(),
  mustFollowAdapter(trickPhase.getMustFollow()),
  trickPhase.getFieldList().map(card => ({
    playerId: card.getPlayerId(),
    card: card.getCard()!
  })),
  trickPhase.getTrick(),
  scoreBoardAdapter(trickPhase.getScoreBoard()!)
  )
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
  const gameRoomId = state.getGameRoomId();
  const roomOwnerId = state.getRoomOwnerId();
  const deck = new Map<string, Card>();
  state.getDeckList().forEach(card => {
    deck.set(card.getCardId(), card);
  });

  switch (state.getStateCase()) {
    case ProtoGameState.StateCase.START_PHASE:
      const startPhase = state.getStartPhase()!;
      startPhase.getDealerId()

      return new StartPhase(
      deck,
      gameRoomId, myPlayerId,
      new GameEventStack(),
      gameRuleAdapter(startPhase.getGameRule()!),
      roomOwnerId,
      startPhase.getPlayerIdsList()
      );

    case ProtoGameState.StateCase.BIDDING_PHASE:
      const biddingPhase = state.getBiddingPhase()!;

      return new BiddingPhase(
      deck,
      gameRoomId, myPlayerId,
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
      const trickPhase = state.getTrickPhase()!;

      return trickPhaseAdapter(gameRoomId, myPlayerId, roomOwnerId, deck, trickPhase);

    case ProtoGameState.StateCase.NEXT_TRICK_LEAD_PLAYER_CHANGING:
      const nextTrickLeadPlayerChanging = state.getNextTrickLeadPlayerChanging()!;

      return new NextTrickLeadPlayerChangingPhase(
      trickPhaseAdapter(gameRoomId, myPlayerId, roomOwnerId, deck, nextTrickLeadPlayerChanging.getTrickPhase()!),
      nextTrickLeadPlayerChanging.getChangingPlayerId()
      );

    case ProtoGameState.StateCase.HAND_CHANGE_WAITING:
      const handChangeWaiting = state.getHandChangeWaiting()!;

      return new HandChangeWaitingPhase(
      trickPhaseAdapter(gameRoomId, myPlayerId, roomOwnerId, deck, handChangeWaiting.getTrickPhase()!),
      handChangeWaiting.getChangingPlayerId()
      );

    case ProtoGameState.StateCase.FUTURE_PREDICATE_WAITING:
      const futurePredicateWaiting = state.getFuturePredicateWaiting()!;

      return new FuturePredicateWaitingPhase(
      trickPhaseAdapter(gameRoomId, myPlayerId, roomOwnerId, deck, futurePredicateWaiting.getTrickPhase()!),
      futurePredicateWaiting.getPredicatingPlayerId()
      );

    case ProtoGameState.StateCase.BID_DECLARE_CHANGE_WAITING:
      const bidDeclareChangeWaiting = state.getBidDeclareChangeWaiting()!;

      return new BidDeclareChangeWaitingPhase(
      trickPhaseAdapter(gameRoomId, myPlayerId, roomOwnerId, deck, bidDeclareChangeWaiting.getTrickPhase()!),
      bidDeclareChangeWaiting.getChangingPlayerId()
      );

    case ProtoGameState.StateCase.FINISHED_PHASE:
      const finished = state.getFinishedPhase()!;

      return new FinishedPhase(
      deck,
      gameRoomId,
      myPlayerId,
      roomOwnerId,
      new GameEventStack(),
      gameRuleAdapter(finished.getGameRule()!),
      finished.getLastWinnerId(),
      scoreBoardAdapter(finished.getScoreBoard()!)
      );

    default:
      throw new Error("illegal state detected");
  }

}