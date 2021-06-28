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

export class StartPhase implements GameState {

  constructor(
  private myPlayerId: string,
  private eventStack: GameEventStack,
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

      case GameEvent.EventCase.GAME_STARTED:
        const gameStarted = event.getGameStarted()!;

        return new BiddingPhase(
        this.myPlayerId,
        this.eventStack, this.rule,
        this.roomOwnerId, this.roomOwnerId,
        gameStarted.getPlayerIdList(), null, 0,
        []
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

}

export class BiddingPhase implements GameState {

  constructor(
  private myPlayerId: string,
  private eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly roomOwnerId: string,
  readonly dealerId: string,
  readonly playerIds: string[],
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
        const trickingPlayers = trickStarted.getJoinedPlayersList()
        .map<TrickingPlayer>(joined => ({
          playerId: joined.getPlayerId(),
          declaredBid: joined.getDeclaredBid(),
          tookTrick: 0,
          card: joined.getCard(),
          tookBonus: 0,
        }));

        return new TrickPhase(
        this.myPlayerId,
        this.eventStack, this.rule, this.roomOwnerId, this.round, this.dealerId,
        trickingPlayers, trickStarted.getDeck(),
        0, null, [], 1, this.scoreBoard
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

}

export type TrickingPlayer = {
  playerId: string;
  declaredBid: number;
  tookTrick: number;
  card: number;
  tookBonus: number
}

export enum MustFollow {
  GREEN,
  YELLOW,
  PURPLE,
  BLACK
}

export class TrickPhase implements GameState {

  constructor(
  private myPlayerId: string,
  readonly eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly roomOwnerId: string,
  readonly round: number,
  readonly dealerId: string,
  readonly players: TrickingPlayer[],
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

      case GameEvent.EventCase.NEXT_TRICK_LEAD_PLAYER_CHANGEABLE_NOTICE:
        return new NextTrickLeadPlayerChangingPhase(this);

      case GameEvent.EventCase.HAND_CHANGE_AVAILABLE_NOTICE:
        return new HandChangeWaitingPhase(this);

      case GameEvent.EventCase.FUTURE_PREDICATE_AVAILABLE:
        return new FuturePredicateWaitingPhase(this);

      case GameEvent.EventCase.DECLARE_BID_CHANGE_AVAILABLE:
        return new BidDeclareChangeWaitingPhase(this);

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

      case GameEvent.EventCase.BIDDING_STARTED:
        const biddingStarted = event.getBiddingStarted()!;

        return new BiddingPhase(
        this.myPlayerId,
        this.eventStack, this.rule,
        this.roomOwnerId, biddingStarted.getDealerId(),
        this.players.map(player => player.playerId), null, biddingStarted.getRound(),
        this.scoreBoard
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
        this.myPlayerId, this.eventStack, this.rule,
        gameFinished.getGameWinnerId(), gameScore
        );

      default:
        return this;
    }
  }

  getGameEvents = this.eventStack.getGameEvents;

}

export class NextTrickLeadPlayerChangingPhase implements GameState {

  constructor(private gameState: TrickPhase) {
  }

  applyEvent(event: GameEvent): GameState {
    return this;
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class HandChangeWaitingPhase implements GameState {

  constructor(private gameState: TrickPhase) {
  }

  applyEvent(event: GameEvent): GameState {
    return this;
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class FuturePredicateWaitingPhase implements GameState {

  constructor(private gameState: TrickPhase) {
  }

  applyEvent(event: GameEvent): GameState {
    return this;
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class BidDeclareChangeWaitingPhase implements GameState {

  constructor(private gameState: TrickPhase) {
  }

  applyEvent(event: GameEvent): GameState {
    return this;
  }

  getGameEvents = this.gameState.eventStack.getGameEvents;

}

export class FinishedPhase implements GameState {

  constructor(
  private myPlayerId: string,
  readonly eventStack: GameEventStack,
  readonly rule: GameRule,
  readonly winnerId: string,
  readonly gameScore: Map<string, { score: number, bonus: number }>[],
  ) {
  }

  applyEvent(event: GameEvent): GameState {
    return this;
  }

  getGameEvents = this.eventStack.getGameEvents;

}
