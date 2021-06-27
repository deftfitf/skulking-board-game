import {GameRoomCreateRequest} from "../models/Models";
import {Card, GameCommand, GameRule} from "../proto/GameServerService_pb";

export class GameServerSocketClient {

  constructor(
  private readonly socket: WebSocket,
  private readonly gameRoomId: string
  ) {
  }

  sendCreateRoom = (request: GameRoomCreateRequest) => {
    const gameRule = new GameRule()
    .setRoomSize(request.roomSize)
    .setNOfRounds(request.nOfRounds)
    .setDeckType(this.adaptDeckType(request.deckType));

    const initMessage = this.newGameCommand()
    .setCreateRoom(new GameCommand.CreateRoom().setGameRule(gameRule));

    this.send(initMessage);
  }

  sendJoinRoom = () => {
    const joinMessage = this.newGameCommand()
    .setJoin(new GameCommand.Join());

    this.send(joinMessage);
  }

  sendNewConnection = () => {
    const newConnectionMessage = this.newGameCommand()
    .setNewConnection(new GameCommand.NewConnection())

    this.send(newConnectionMessage);
  }

  sendLeaveRoom = () => {
    const leaveRoomMessage = this.newGameCommand()
    .setLeave(new GameCommand.Leave());

    this.send(leaveRoomMessage);
  }

  sendGameStart = () => {
    const gameStartMessage = this.newGameCommand()
    .setGameStart(new GameCommand.GameStart());

    this.send(gameStartMessage);
  }

  sendBidDeclare = (bid: number) => {
    const bidDeclare = new GameCommand.BidDeclare().setBid(bid);

    const bidDeclareMessage = this.newGameCommand().setBidDeclare(bidDeclare);

    this.send(bidDeclareMessage);
  }

  sendPlayCard = (card: Card) => {
    const playCard = new GameCommand.PlayCard().setCard(card);
    const playCardMessage = this.newGameCommand().setPlayCard(playCard);

    this.send(playCardMessage);
  }

  sendNextTrickLeadPlayerChange = (newLeadPlayerId: string) => {
    const nextTrickLeadPlayerChangeMessage = this.newGameCommand()
    .setNextTrickLeadPlayerChange(new GameCommand.NextTrickLeadPlayerChange().setNewLeadPlayerId(newLeadPlayerId));

    this.send(nextTrickLeadPlayerChangeMessage);
  }

  sendPlayerHandChange = (cardIds: string[]) => {
    const playerHandChangeMessage = this.newGameCommand()
    .setPlayerHandChange(new GameCommand.PlayerHandChange().setCardIdList(cardIds));

    this.send(playerHandChangeMessage);
  }

  sendFuturePredicateFinish = () => {
    const futurePredicateFinishMessage = this.newGameCommand()
    .setFuturePredicateFinish(new GameCommand.FuturePredicateFinish());

    this.send(futurePredicateFinishMessage);
  }

  sendBidDeclareChange = (newBid: number) => {
    const bidDeclareChangeMessage = this.newGameCommand()
    .setBidDeclareChange(new GameCommand.BidDeclareChange().setBid(newBid));

    this.send(bidDeclareChangeMessage)
  }

  sendReplayGame = () => {
    const replayGameMessage = this.newGameCommand()
    .setReplayGame(new GameCommand.ReplayGame());

    this.send(replayGameMessage);
  }

  sendEndGame = () => {
    const endGameMessage = this.newGameCommand()
    .setEndGame(new GameCommand.EndGame());

    this.send(endGameMessage);
  }

  sendSnapshotRequest = () => {
    const snapshotRequestMessage = this.newGameCommand()
    .setSnapshotRequest(new GameCommand.SnapshotRequest());

    this.send(snapshotRequestMessage);
  }

  private send = (gameCommand: GameCommand) =>
  this.socket.send(gameCommand.serializeBinary());

  private newGameCommand = () => {
    return new GameCommand().setGameRoomId(this.gameRoomId);
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