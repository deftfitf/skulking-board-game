import {GameRoomCreateRequest} from "../models/Models";
import {Card, GameCommand, GameRule} from "../proto/GameServerService_pb";

export class GameServerSocketClient {

  constructor(
  private readonly socket: WebSocket
  ) {
  }

  sendCreateRoom = (request: GameRoomCreateRequest) => {
    const gameRule = new GameRule()
    .setRoomSize(request.roomSize)
    .setNOfRounds(request.nOfRounds)
    .setDeckType(this.adaptDeckType(request.deckType));

    const initMessage = new GameCommand()
    .setCreateRoom(new GameCommand.CreateRoom().setGameRule(gameRule));

    this.send(initMessage);
  }

  sendJoinRoom = (gameRoomId: string) => {
    const joinMessage = this.newGameCommand(gameRoomId)
    .setJoin(new GameCommand.Join());

    this.send(joinMessage);
  }

  sendNewConnection = (gameRoomId: string) => {
    const newConnectionMessage = this.newGameCommand(gameRoomId)
    .setNewConnection(new GameCommand.NewConnection())

    this.send(newConnectionMessage);
  }

  sendLeaveRoom = (gameRoomId: string) => {
    const leaveRoomMessage = this.newGameCommand(gameRoomId)
    .setLeave(new GameCommand.Leave());

    this.send(leaveRoomMessage);
  }

  sendGameStart = (gameRoomId: string) => {
    const gameStartMessage = this.newGameCommand(gameRoomId)
    .setGameStart(new GameCommand.GameStart());

    this.send(gameStartMessage);
  }

  sendBidDeclare = (gameRoomId: string, bid: number) => {
    const bidDeclare = new GameCommand.BidDeclare().setBid(bid);

    const bidDeclareMessage = this.newGameCommand(gameRoomId).setBidDeclare(bidDeclare);

    this.send(bidDeclareMessage);
  }

  sendPlayCard = (gameRoomId: string, card: Card) => {
    const playCard = new GameCommand.PlayCard().setCard(card);
    const playCardMessage = this.newGameCommand(gameRoomId).setPlayCard(playCard);

    this.send(playCardMessage);
  }

  sendNextTrickLeadPlayerChange = (gameRoomId: string, newLeadPlayerId: string) => {
    const nextTrickLeadPlayerChangeMessage = this.newGameCommand(gameRoomId)
    .setNextTrickLeadPlayerChange(new GameCommand.NextTrickLeadPlayerChange().setNewLeadPlayerId(newLeadPlayerId));

    this.send(nextTrickLeadPlayerChangeMessage);
  }

  sendPlayerHandChange = (gameRoomId: string, cardIds: string[]) => {
    const playerHandChangeMessage = this.newGameCommand(gameRoomId)
    .setPlayerHandChange(new GameCommand.PlayerHandChange().setCardIdList(cardIds));

    this.send(playerHandChangeMessage);
  }

  sendFuturePredicateFinish = (gameRoomId: string) => {
    const futurePredicateFinishMessage = this.newGameCommand(gameRoomId)
    .setFuturePredicateFinish(new GameCommand.FuturePredicateFinish());

    this.send(futurePredicateFinishMessage);
  }

  sendBidDeclareChange = (gameRoomId: string, newBid: number) => {
    const bidDeclareChangeMessage = this.newGameCommand(gameRoomId)
    .setBidDeclareChange(new GameCommand.BidDeclareChange().setBid(newBid));

    this.send(bidDeclareChangeMessage)
  }

  sendReplayGame = (gameRoomId: string) => {
    const replayGameMessage = this.newGameCommand(gameRoomId)
    .setReplayGame(new GameCommand.ReplayGame());

    this.send(replayGameMessage);
  }

  sendEndGame = (gameRoomId: string) => {
    const endGameMessage = this.newGameCommand(gameRoomId)
    .setEndGame(new GameCommand.EndGame());

    this.send(endGameMessage);
  }

  sendSnapshotRequest = (gameRoomId: string) => {
    const snapshotRequestMessage = this.newGameCommand(gameRoomId)
    .setSnapshotRequest(new GameCommand.SnapshotRequest());

    this.send(snapshotRequestMessage);
  }

  private send = (gameCommand: GameCommand) =>
  this.socket.send(gameCommand.serializeBinary());

  private newGameCommand = (gameRoomId: string) => {
    return new GameCommand().setGameRoomId(gameRoomId);
  }

  private adaptDeckType: (deckType: "STANDARD" | "EXPANSION") => GameRule.DeckType = deckType => {
    if (deckType == "STANDARD") {
      return GameRule.DeckType.STANDARD;
    } else if (deckType == "EXPANSION") {
      return GameRule.DeckType.EXPANSION;
    }
    throw new Error("Unknown deck type detected");
  }

}