import React, {useEffect, useState} from 'react';

import {useHistory, useLocation, useParams} from 'react-router-dom';
import {GameRoomCreateRequest} from "../models/Models";
import {GameServerSocketClient} from "../clients/GameServerSocketClient";
import {GameEvent} from "../proto/GameServerService_pb";
import {
  BidDeclareChangeWaitingPhase,
  BiddingPhase,
  FinishedPhase,
  FuturePredicateWaitingPhase,
  GameEnded,
  GameState,
  HandChangeWaitingPhase,
  NextTrickLeadPlayerChangingPhase,
  StartPhase,
  TrickPhase,
  WaitForInitialize
} from "../models/GameModels";

export interface GameRoomProps {
  createRequest?: GameRoomCreateRequest;
  playerId?: string;
  isReConnect: boolean;
}

const StartPhaseBoard = (props: { gameState: StartPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const BiddingPhaseBoard = (props: { gameState: BiddingPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const TrickPhaseBoard = (props: { gameState: TrickPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const NextTrickLeadPlayerChangingPhaseBoard = (props: { gameState: NextTrickLeadPlayerChangingPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const HandChangeWaitingPhaseBoard = (props: { gameState: HandChangeWaitingPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const FuturePredicateWaitingPhaseBoard = (props: { gameState: FuturePredicateWaitingPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const BidDeclareChangeWaitingPhaseBoard = (props: { gameState: BidDeclareChangeWaitingPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};

const FinishedPhaseBoard = (props: { gameState: FinishedPhase }) => {

  return (
  <React.Fragment>

  </React.Fragment>
  );
};


const GameRoom = () => {
  const props = useParams<{ _gameRoomId: string } | undefined>();
  const [gameRoomId, setGameRoomId] = useState(props?._gameRoomId);
  const location = useLocation<GameRoomProps>();
  const history = useHistory();
  const {state} = location;
  const [gameState, setGameState] =
  useState<GameState>(new WaitForInitialize(gameRoomId => setGameRoomId(gameRoomId)));

  useEffect(() => {
    let socketReleaseFunction: () => void;
    if (state.createRequest) {
      const createRequest = state.createRequest;
      socketReleaseFunction = connectionConfigure(client => {
        client.sendCreateRoom(createRequest);
      });
    } else if (state.isReConnect) {
      socketReleaseFunction = connectionConfigure(client => {
        client.sendNewConnection(gameRoomId!);
        client.sendSnapshotRequest(gameRoomId!);
      });
    } else {
      socketReleaseFunction = connectionConfigure(client => {
        client.sendJoinRoom(gameRoomId!);
        client.sendSnapshotRequest(gameRoomId!);
      });
    }

    return () => {
      socketReleaseFunction();
    };
  }, []);

  const connectionConfigure = (onOpen: (client: GameServerSocketClient) => void) => {
    const port = 8080
    const socket = new WebSocket(`ws://localhost:${port}/gameserver`);
    const gameServerSocketClient = new GameServerSocketClient(socket);

    socket.onopen = (ev: Event) => {
      console.log("connection opened");
      onOpen(gameServerSocketClient);
    };

    socket.onclose = (ev: CloseEvent) => {
      console.info("connection closed");
    };

    socket.onerror = (ev: Event) => {
      console.error(`connection error: ${ev}`);
    };

    socket.onmessage = (ev: MessageEvent) => {
      ev.data.arrayBuffer().then((buffer: Uint8Array) => {
        const gameEvent = GameEvent.deserializeBinary(buffer);
        setGameState(oldState => oldState?.applyEvent(gameEvent));
        console.log(gameEvent);
      });
    };

    return () => socket.close();
  };

  if (gameState instanceof GameEnded) {
    history.push('/');
    return null;
  }

  return (
  <React.Fragment>
    {`Game Room ${gameRoomId}`}
    {gameState instanceof StartPhase && <StartPhaseBoard gameState={gameState}/>}
    {gameState instanceof BiddingPhase && <BiddingPhaseBoard gameState={gameState}/>}
    {gameState instanceof TrickPhase && <TrickPhaseBoard gameState={gameState}/>}
    {gameState instanceof NextTrickLeadPlayerChangingPhase &&
    <NextTrickLeadPlayerChangingPhaseBoard gameState={gameState}/>}
    {gameState instanceof HandChangeWaitingPhase && <HandChangeWaitingPhaseBoard gameState={gameState}/>}
    {gameState instanceof FuturePredicateWaitingPhase && <FuturePredicateWaitingPhaseBoard gameState={gameState}/>}
    {gameState instanceof BidDeclareChangeWaitingPhase && <BidDeclareChangeWaitingPhaseBoard gameState={gameState}/>}
    {gameState instanceof FinishedPhase && <FinishedPhaseBoard gameState={gameState}/>}
  </React.Fragment>
  );
}
;

export default GameRoom;