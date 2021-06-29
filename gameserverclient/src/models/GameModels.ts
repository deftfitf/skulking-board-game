import {Card, GameEvent} from "../proto/GameServerService_pb";

export enum DeckType {
  STANDARD,
  EXPANSION
}

export type GameRule = {
  readonly roomSize: number;
  readonly nOfRounds: number;
  readonly deckType: DeckType;
}

export class GameEventStack {

  private eventStack: string[] = [];

  getGameEvents: () => string[] = () => {
    return [...this.eventStack];
  };

  appendGameEvent: (gameEvent: GameEvent) => void = (gameEvent) => {
    this.eventStack.push(this.convertEventToString(gameEvent));
  }

  private convertEventToString = (gameEvent: GameEvent) => {
    switch (gameEvent.getEventCase()) {
      default:
        return `${gameEvent.getEventCase()}イベントが発生しました`
    }
  }

}

export interface GameState {

  getGameEvents(): string[];

  applyEvent(event: GameEvent): GameState;

}

const createBiddingPhase = (
myPlayerId: string,
eventStack: GameEventStack,
gameRule: GameRule,
roomOwnerId: string,
scoreBoard: Map<string, { score: number, bonus: number }>[],
event: GameEvent.RoundStarted
) => {
  const biddingPlayers = event.getJoinedPlayersList()
  .map(player => ({
    playerId: player.getPlayerId(),
    card: player.getCard()
  }));

  return new BiddingPhase(
  myPlayerId,
  eventStack, gameRule,
  roomOwnerId, roomOwnerId,
  event.getDeck(), biddingPlayers,
  event.getCardList(),
  null, 1,
  scoreBoard
  );
}

export class StartPhase implements GameState {

  constructor(
  readonly myPlayerId: string,
  readonly eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly roomOwnerId: string,
  readonly playerIds: string[]
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    this.eventStack.appendGameEvent(event);

    switch (event.getEventCase()) {
      case GameEvent.EventCase.A_PLAYER_JOINED:
        const aPlayerJoined = event.getAPlayerJoined()!;

        this.playerIds.push(aPlayerJoined.getPlayerId())
        return this;

      case GameEvent.EventCase.A_PLAYER_LEFT:
        const aPlayerLeft = event.getAPlayerLeft()!;

        return new StartPhase(
        this.myPlayerId,
        this.eventStack, this.rule, this.roomOwnerId,
        this.playerIds.filter(id => aPlayerLeft.getPlayerId() !== id)
        );

      case GameEvent.EventCase.ROUND_STARTED:
        return createBiddingPhase(
        this.myPlayerId, this.eventStack, this.rule,
        this.roomOwnerId, [], event.getRoundStarted()!
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

}

export type BiddingPlayer = {
  readonly playerId: string;
  readonly card: number;
}

export class BiddingPhase implements GameState {

  constructor(
  readonly myPlayerId: string,
  readonly eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly roomOwnerId: string,
  readonly dealerId: string,
  readonly deck: number,
  readonly players: BiddingPlayer[],
  readonly myCardIds: string[],
  private myBid: number | null,
  private round: number,
  readonly scoreBoard: Map<string, { score: number, bonus: number }>[]
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    this.eventStack.appendGameEvent(event);

    switch (event.getEventCase()) {
      case GameEvent.EventCase.A_PLAYER_BID_DECLARED:
        const bidDeclared = event.getAPlayerBidDeclared()!;
        this.myBid = bidDeclared.getBidDeclared();

        return this;

      case GameEvent.EventCase.TRICK_STARTED:
        const trickStarted = event.getTrickStarted()!;
        const trickingPlayers = trickStarted.getBidPlayersList()
        .map<TrickingPlayer>(joined => ({
          playerId: joined.getPlayerId(),
          declaredBid: joined.getBid(),
          tookTrick: 0,
          card: joined.getCard(),
          tookBonus: 0,
        }));

        return new TrickPhase(
        this.myPlayerId,
        this.eventStack, this.rule, this.roomOwnerId, this.round, this.dealerId,
        trickingPlayers, this.myCardIds, this.deck,
        0, null, [], 1, this.scoreBoard
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

}

export type TrickingPlayer = {
  readonly playerId: string;
  readonly declaredBid: number;
  readonly tookTrick: number;
  readonly card: number;
  readonly tookBonus: number
}

export enum MustFollow {
  GREEN,
  YELLOW,
  PURPLE,
  BLACK
}

export class TrickPhase implements GameState {

  constructor(
  readonly myPlayerId: string,
  readonly eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly roomOwnerId: string,
  readonly round: number,
  private dealerId: string,
  readonly players: TrickingPlayer[],
  private myCardIds: string[],
  readonly deck: number,
  readonly stack: number,
  readonly mustFollow: MustFollow | null,
  readonly field: { playerId: string, card: Card }[],
  readonly trick: number,
  readonly scoreBoard: Map<string, { score: number, bonus: number }>[],
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    this.eventStack.appendGameEvent(event);

    switch (event.getEventCase()) {
      case GameEvent.EventCase.A_PLAYER_TRICK_PLAYED:
        const played = event.getAPlayerTrickPlayed()!;

        const idx = this.players.findIndex(player => player.playerId == played.getPlayerId());
        this.players[idx] = {
          ...this.players[idx],
          card: this.players[idx].card - 1,
        }

        this.field.push({
          playerId: played.getPlayerId(),
          card: played.getPlayedCard()!
        });

        return this;

      case GameEvent.EventCase.NEXT_TRICK_LEAD_PLAYER_CHANGEABLE_NOTICE:
        const nextTrickLeadPlayerChangeableNotice = event.getNextTrickLeadPlayerChangeableNotice()!;

        return new NextTrickLeadPlayerChangingPhase(this, nextTrickLeadPlayerChangeableNotice.getPlayerId());

      case GameEvent.EventCase.HAND_CHANGE_AVAILABLE_NOTICE:
        const handChangeAvailableNotice = event.getHandChangeAvailableNotice()!;

        return new HandChangeWaitingPhase(this,
        handChangeAvailableNotice.getPlayerId(),
        handChangeAvailableNotice.getDrawCardsList());

      case GameEvent.EventCase.FUTURE_PREDICATE_AVAILABLE:
        const futurePredicateNotice = event.getFuturePredicateAvailable()!;
        const targetPlayerId = futurePredicateNotice.getPlayerId();

        return new FuturePredicateWaitingPhase(this,
        targetPlayerId,
        targetPlayerId == this.myPlayerId ? futurePredicateNotice.getDeckCardList() : undefined);

      case GameEvent.EventCase.DECLARE_BID_CHANGE_AVAILABLE:
        const declareBidChangeAvailable = event.getDeclareBidChangeAvailable()!;

        return new BidDeclareChangeWaitingPhase(this,
        declareBidChangeAvailable.getPlayerId(),
        declareBidChangeAvailable.getMax(),
        declareBidChangeAvailable.getMin());

      case GameEvent.EventCase.ROUND_FINISHED:
        const roundFinished = event.getRoundFinished()!;
        const roundScoreMap = new Map<string, { score: number, bonus: number }>();
        roundFinished.getRoundScoreMap().forEach((score, key) => {
          roundScoreMap.set(key, {
            score: score.getScore(),
            bonus: score.getBonus(),
          });
        });

        this.scoreBoard.push(roundScoreMap);
        return this;

      case GameEvent.EventCase.ROUND_STARTED:
        return createBiddingPhase(
        this.myPlayerId, this.eventStack, this.rule,
        this.roomOwnerId, this.scoreBoard, event.getRoundStarted()!
        );

      case GameEvent.EventCase.GAME_FINISHED:
        const gameFinished = event.getGameFinished()!;
        const gameScore: Map<string, { score: number, bonus: number }>[] = [];

        gameFinished.getScoreBoard()!
        .getRoundScoresList()
        .forEach(roundScore => {
          const roundScoreMap = new Map<string, { score: number, bonus: number }>();
          roundScore.getRoundScoreMap().forEach((score, key) => {
            roundScoreMap.set(key, {
              score: score.getScore(),
              bonus: score.getBonus(),
            });
          });
        });

        return new FinishedPhase(
        this.myPlayerId, this, this.eventStack, this.rule,
        gameFinished.getGameWinnerId(), gameScore
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

  changeLeadPlayerId = (newLeadPlayerId: string) => this.dealerId = newLeadPlayerId;

  addMyCards = (cards: string[]) => this.myCardIds.push(...cards);

  removeMyCards = (cards: string[]) =>
  this.myCardIds = this.myCardIds.filter(cardId => cards.includes(cardId));

  changeBidDeclare = (playerId: string, newBid: number) => {
    const idx = this.players.findIndex(player => player.playerId == playerId);
    this.players[idx] = {
      ...this.players[idx],
      declaredBid: newBid
    }
  }

  getDealerId = () => this.dealerId;

  getMyCardIds = () => [...this.myCardIds];

}

export class NextTrickLeadPlayerChangingPhase implements GameState {

  constructor(
  readonly gameState: TrickPhase,
  readonly changingPlayerId: string,
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    switch (event.getEventCase()) {
      case GameEvent.EventCase.NEXT_TRICK_LEAD_PLAYER_CHANGED:
        const changed = event.getNextTrickLeadPlayerChanged()!;
        this.gameState.changeLeadPlayerId(changed.getNewLeadPlayerId());

        return this.gameState;

      default:
        return this;
    }
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class HandChangeWaitingPhase implements GameState {

  constructor(
  readonly gameState: TrickPhase,
  readonly changingPlayerId: string,
  readonly drawCardIds?: string[],
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    switch (event.getEventCase()) {
      case GameEvent.EventCase.PLAYER_HAND_CHANGED:
        const handChanged = event.getPlayerHandChanged()!;

        if (handChanged.getPlayerId() == this.gameState.myPlayerId) {
          this.gameState.addMyCards(this.drawCardIds!);
          this.gameState.removeMyCards(handChanged.getReturnCardsList());
        }

        return this.gameState;

      default:
        return this;
    }
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class FuturePredicateWaitingPhase implements GameState {

  constructor(
  readonly gameState: TrickPhase,
  readonly predicatingPlayerId: string,
  readonly deckCards?: string[],
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    switch (event.getEventCase()) {
      case GameEvent.EventCase.FUTURE_PREDICATED:
        return this.gameState;

      default:
        return this;
    }
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class BidDeclareChangeWaitingPhase implements GameState {

  constructor(
  readonly gameState: TrickPhase,
  readonly changingPlayerId: string,
  readonly max: number,
  readonly min: number,
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    switch (event.getEventCase()) {
      case GameEvent.EventCase.BID_DECLARE_CHANGED:
        const bidDeclareChanged = event.getBidDeclareChanged()!;

        this.gameState.changeBidDeclare(
        bidDeclareChanged.getChangedPlayerId(),
        bidDeclareChanged.getChangedBid());

        return this.gameState;

      default:
        return this;
    }
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class FinishedPhase implements GameState {

  constructor(
  readonly myPlayerId: string,
  readonly lastState: TrickPhase,
  readonly eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly winnerId: string,
  readonly gameScore: Map<string, { score: number, bonus: number }>[],
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    switch (event.getEventCase()) {
      case GameEvent.EventCase.GAME_ENDED:
        return new GameEnded();

      case GameEvent.EventCase.ROUND_STARTED:
        return createBiddingPhase(
        this.myPlayerId, this.eventStack, this.rule,
        this.lastState.roomOwnerId, [], event.getRoundStarted()!
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

}

export class GameEnded implements GameState {

  applyEvent(event: GameEvent): GameState {
    return this;
  }

  getGameEvents(): string[] {
    return [];
  }

}