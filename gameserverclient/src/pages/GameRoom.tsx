import React, {useEffect, useState} from 'react';

import {useHistory, useLocation, useParams} from 'react-router-dom';
import {GamePlayer} from "../models/Models";
import {GameServerSocketClient} from "../clients/GameServerSocketClient";
import {Card, GameEvent} from "../proto/GameServerService_pb";
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
import {Avatar, Box, Button, ButtonBase, Container, Grid, makeStyles, Paper, Typography} from "@material-ui/core";

export interface GameRoomProps {
  isJoin: boolean;
}

const useStyle = makeStyles((theme) => ({
  root: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  },
  gameBoardContainer: {
    display: 'grid',
    width: '100vw',
    height: '100vh',
  },
  card: {
    width: '120px',
    height: '192px',
  }
}));

const SkulkingCard = (props: { cardId: string }) => {
  const classes = useStyle();

  return <Paper className={classes.card}>
    {`${props.cardId}`}
  </Paper>
}

const SkulkingCardTricking = (props: {
  gameRoomId: string,
  _card: Card,
  isMyHandCard: boolean,
  isMyTarn: boolean,
  webSocketClient: GameServerSocketClient
}) => {
  const classes = useStyle();
  const [card, setCard] = useState<Card>(props._card);

  const onPlayCard = () => {
    props.webSocketClient.sendPlayCard(props.gameRoomId, card);
  }

  let cardContent: JSX.Element;
  switch (card.getCardCase()) {
    case Card.CardCase.TIGRESS:
      // TODO: 選択制popup
      cardContent = <Paper className={classes.card}>
        {`${card.getCardId()}`}
      </Paper>
      break;

    case Card.CardCase.RASCAL_OF_ROATAN:
      // TODO: 選択制popup
      cardContent = <Paper className={classes.card}>
        {`${card.getCardId()}`}
      </Paper>
      break;

    default:
      cardContent = <Paper className={classes.card}>
        {`${card.getCardId()}`}
      </Paper>
      break;
  }

  if (props.isMyHandCard && props.isMyTarn) {
    return <ButtonBase onClick={onPlayCard}>
      cardContent
    </ButtonBase>
  }

  return cardContent;
}

const WaitForInitializeBoard = (props: { gameState: WaitForInitialize }) => {

  return (
  <React.Fragment>
    Waiting !
  </React.Fragment>
  );
};

const StartPhaseBoard = (props: { gameState: StartPhase, webSocketClient: GameServerSocketClient }) => {

  const onLeaveRoom = () => {
    props.webSocketClient.sendLeaveRoom(props.gameState.gameRoomId);
  };

  const onStartGame = () => {
    props.webSocketClient.sendGameStart(props.gameState.gameRoomId);
  };

  return (
  <Box>
    <Grid container spacing={3} xs={12}>
      <Grid container item xs={12} spacing={2}>
        <Typography variant='h4'>ロビー</Typography>
      </Grid>
      <Grid container item xs={12} spacing={2}>
        <Grid item xs={12}>
          <Typography variant='h5'>現在の参加者</Typography>
        </Grid>
        {props.gameState.playerIds.map(playerId =>
        <Grid item xs={3}>
          <Paper>
            <Avatar/>
            <Typography>
              {`${playerId}`}
            </Typography>
          </Paper>
        </Grid>
        )}
      </Grid>
      <Grid container justify='center' item xs={12}>
        <Grid item xs={3}>
          {props.gameState.isMeRoomOwner && props.gameState.canStartGame &&
          <Button variant='contained' color='primary' onClick={onStartGame}>
              ゲームを開始する
          </Button>
          }
          <Button variant='contained' onClick={onLeaveRoom}>
            退室する
          </Button>
        </Grid>
      </Grid>
    </Grid>
  </Box>
  );
};

const BiddingPhaseBoard = (props: { gameState: BiddingPhase, webSocketClient: GameServerSocketClient }) => {

  const onSendBid = (bid: number) => {
    props.webSocketClient.sendBidDeclare(props.gameState.gameRoomId, bid);
  }

  return (
  <Box>
    <Grid container spacing={3}>
      <Typography variant='h4'>ビッドフェーズ</Typography>
      <Grid container item xs={12} spacing={2}>
        <Grid item xs={12}>
          <Typography variant='h5'>プレイヤー</Typography>
        </Grid>
        {props.gameState.players.map(player =>
        <Grid item xs={3}>
          <Paper>
            <Avatar/>
            <Typography>
              {`${player.playerId}`}
            </Typography>
            <Typography>
              {`${player.card}`}
            </Typography>
            <Typography>
              {player.playerId != props.gameState.myPlayerId
              ? (player.isBid ? 'ビッド済み' : '未ビット')
              : (player.isBid ? props.gameState.myBid : '未ビッド')}
            </Typography>
          </Paper>
        </Grid>
        )}
      </Grid>
      <Typography variant='h5'>手札</Typography>
      <Grid container item xs={12} spacing={2}>
        {props.gameState.myCardIds.map(myCardId =>
        <Grid item>
          <SkulkingCard cardId={myCardId}/>
        </Grid>
        )}
      </Grid>
      <Typography variant='h5'>ビッド</Typography>
      <Grid container item xs={12} spacing={2}>
        {[...new Array(props.gameState.round + 1).keys()]
        .map(bid => <Grid item>
          <Button
          variant='contained' color='secondary' value={bid}
          onClick={() => onSendBid(bid)}
          >
            {bid}
          </Button>
        </Grid>
        )}
      </Grid>
    </Grid>
  </Box>
  );
};

const TrickPhaseBoard = (props: { gameState: TrickPhase, webSocketClient: GameServerSocketClient }) => {

  const onPlayCard = (card: Card) => {
    props.webSocketClient.sendPlayCard(props.gameState.gameRoomId, card);
  };

  return (
  <Box>
    <Grid container spacing={3}>
      <Typography variant='h4'>トリックフェーズ</Typography>
      <Grid container item xs={12} spacing={2}>
        <Grid item xs={12}>
          <Typography variant='h5'>プレイヤー</Typography>
        </Grid>
        {props.gameState.players.map(player =>
        <Grid item xs={3}>
          <Paper>
            <Avatar/>
            <Typography>
              {`プレイヤー: ${player.playerId}`}
            </Typography>
            <Typography>
              {`カード: ${player.card}`}
            </Typography>
            <Typography>
              {`宣言したビッド: ${player.declaredBid}`}
            </Typography>
            <Typography>
              {`勝利したトリック: ${player.tookTrick}`}
            </Typography>
            <Typography>
              {`獲得したボーナス: ${player.tookBonus}`}
            </Typography>
          </Paper>
        </Grid>
        )}
      </Grid>
      <Typography variant='h5'>手札</Typography>
      <Grid container item xs={12} spacing={2}>
        {props.gameState.myCardIds.map(myCardId =>
        <Grid item>
          <SkulkingCardTricking
          gameRoomId={props.gameState.gameRoomId}
          _card={props.gameState.getDeck().get(myCardId)!}
          isMyHandCard={true}
          isMyTarn={props.gameState.isMyTurn()}
          webSocketClient={props.webSocketClient}/>
        </Grid>
        )}
      </Grid>
      <Typography variant='h5'>フィールド</Typography>
      <Grid container item xs={12} spacing={2}>
        {props.gameState.field.map(field =>
        <Grid item>
          <SkulkingCardTricking
          gameRoomId={props.gameState.gameRoomId}
          _card={field.card}
          isMyHandCard={false}
          isMyTarn={props.gameState.isMyTurn()}
          webSocketClient={props.webSocketClient}/>
        </Grid>
        )}
      </Grid>
    </Grid>
  </Box>
  );
};

const NextTrickLeadPlayerChangingPhaseBoard = (props: { gameState: NextTrickLeadPlayerChangingPhase, webSocketClient: GameServerSocketClient }) => {

  return (
  <React.Fragment>
    Next Trick Lead Player Changing!
  </React.Fragment>
  );
};

const HandChangeWaitingPhaseBoard = (props: { gameState: HandChangeWaitingPhase, webSocketClient: GameServerSocketClient }) => {

  return (
  <React.Fragment>
    Hand Change Waiting !
  </React.Fragment>
  );
};

const FuturePredicateWaitingPhaseBoard = (props: { gameState: FuturePredicateWaitingPhase, webSocketClient: GameServerSocketClient }) => {

  return (
  <React.Fragment>
    Future Predicate Waiting !
  </React.Fragment>
  );
};

const BidDeclareChangeWaitingPhaseBoard = (props: { gameState: BidDeclareChangeWaitingPhase, webSocketClient: GameServerSocketClient }) => {

  return (
  <React.Fragment>
    Bid Declare Change Waiting !
  </React.Fragment>
  );
};

const FinishedPhaseBoard = (props: { gameState: FinishedPhase, webSocketClient: GameServerSocketClient }) => {

  return (
  <React.Fragment>
    Finished !
  </React.Fragment>
  );
};


const GameRoom = (props: { gamePlayer: GamePlayer }) => {
  const {gameRoomId} = useParams<{ gameRoomId: string }>();
  const location = useLocation<GameRoomProps>();
  const history = useHistory();
  const {state} = location;
  const [gameState, setGameState] =
  useState<GameState>(new WaitForInitialize(gameRoomId, props.gamePlayer.playerId));
  const [gameServerSocketClient, setGameServerSocketClient] =
  useState<GameServerSocketClient | undefined>();

  useEffect(() => {
    let socketReleaseFunction: () => void;
    if (state === undefined || state.isJoin === undefined || !state.isJoin) {
      socketReleaseFunction = connectionConfigure(client => {
        client.sendNewConnection(gameRoomId!);
      });
    } else {
      socketReleaseFunction = connectionConfigure(client => {
        client.sendJoinRoom(gameRoomId!);
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
    setGameServerSocketClient(gameServerSocketClient);

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
        const gameEvent: GameEvent = GameEvent.deserializeBinary(buffer);
        setGameState(oldState => oldState.applyEvent(gameEvent));
        if (gameEvent.getEventCase() != GameEvent.EventCase.KEEP_ALIVE) {
          console.log(gameEvent.toObject(false));
        }
      });
    };

    return () => socket.close();
  };

  if (gameState instanceof GameEnded) {
    history.push('/');
    return null;
  }

  return (
  <section>
    <Container>
      {gameState instanceof WaitForInitialize && <WaitForInitializeBoard gameState={gameState}/>}
      {gameState instanceof StartPhase &&
      <StartPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof BiddingPhase &&
      <BiddingPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof TrickPhase &&
      <TrickPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof NextTrickLeadPlayerChangingPhase &&
      <NextTrickLeadPlayerChangingPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof HandChangeWaitingPhase &&
      <HandChangeWaitingPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof FuturePredicateWaitingPhase &&
      <FuturePredicateWaitingPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof BidDeclareChangeWaitingPhase &&
      <BidDeclareChangeWaitingPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
      {gameState instanceof FinishedPhase &&
      <FinishedPhaseBoard gameState={gameState} webSocketClient={gameServerSocketClient!}/>}
    </Container>
  </section>
  );
};

export default GameRoom;