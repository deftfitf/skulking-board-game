import React, {useEffect, useState} from 'react';
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
  TrickingPlayer,
  TrickPhase,
  WaitForInitialize
} from "../models/GameModels";
import {
  Avatar,
  Box,
  Button,
  ButtonBase,
  CircularProgress,
  Container,
  Grid,
  makeStyles,
  Paper,
  Typography
} from "@material-ui/core";
import {GamePlayer} from "../models/Models";
import {useHistory, useLocation, useParams} from "react-router-dom";

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
  card: Card,
  isMyHandCard: boolean,
  isMyTarn: boolean,
  webSocketClient: GameServerSocketClient
}) => {
  const classes = useStyle();

  const onPlayCard = () => {
    props.webSocketClient.sendPlayCard(props.gameRoomId, props.card);
  }

  let cardContent: JSX.Element;
  switch (props.card.getCardCase()) {
    case Card.CardCase.TIGRESS:
      // TODO: 選択制popup
      cardContent = <Paper className={classes.card}>
        {`${props.card.getCardId()}`}
      </Paper>
      break;

    case Card.CardCase.RASCAL_OF_ROATAN:
      // TODO: 選択制popup
      cardContent = <Paper className={classes.card}>
        {`${props.card.getCardId()}`}
      </Paper>
      break;

    default:
      cardContent = <Paper className={classes.card}>
        {`${props.card.getCardId()}`}
      </Paper>
      break;
  }

  if (props.isMyHandCard && props.isMyTarn) {
    return <ButtonBase onClick={onPlayCard}>
      {cardContent}
    </ButtonBase>
  }

  return cardContent;
}

const WaitForInitializeBoard = (props: { gameState: WaitForInitialize }) => {

  return (
  <React.Fragment>
    <CircularProgress/>
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
        <Grid item xs={3} key={playerId}>
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
        <Grid item xs={3} key={player.playerId}>
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
        <Grid item key={myCardId}>
          <SkulkingCard cardId={myCardId}/>
        </Grid>
        )}
      </Grid>
      <Typography variant='h5'>ビッド</Typography>
      <Grid container item xs={12} spacing={2}>
        {[...new Array(props.gameState.round + 1).keys()]
        .map(bid => <Grid item key={bid}>
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

  console.log(props.gameState.myCardIds);

  return (
  <Box>
    <Grid container spacing={3}>
      <Typography variant='h4'>トリックフェーズ</Typography>
      <Grid container item xs={12} spacing={2}>
        <Grid item xs={12}>
          <Typography variant='h5'>プレイヤー</Typography>
        </Grid>
        {props.gameState.players.map(player =>
        <Grid item xs={3} key={player.playerId}>
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
        <Grid item key={myCardId}>
          <SkulkingCardTricking
          gameRoomId={props.gameState.gameRoomId}
          card={props.gameState.getDeck().get(myCardId)!}
          isMyHandCard={true}
          isMyTarn={props.gameState.isMyTurn()}
          webSocketClient={props.webSocketClient}/>
        </Grid>
        )}
      </Grid>
      <Typography variant='h5'>フィールド</Typography>
      <Grid container item xs={12} spacing={2}>
        {props.gameState.field.map(field =>
        <Grid item key={field.card.getCardId()}>
          <SkulkingCardTricking
          gameRoomId={props.gameState.gameRoomId}
          card={field.card}
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

const WaitingState = (props: { gameState: TrickPhase, waitFor: string }) => {

  return (
  <Box>
    <Grid container spacing={3}>
      <Typography variant='h4'>{`${props.waitFor}を待機しています`}</Typography>
      <TrickingPlayerBoard players={props.gameState.players}/>
    </Grid>
  </Box>
  )
}

const BidDeclareChangeWaitingPhaseBoard = (props: { gameState: BidDeclareChangeWaitingPhase, webSocketClient: GameServerSocketClient }) => {
  const gameRoomId = props.gameState.gameState.gameRoomId;
  const myPlayerId = props.gameState.gameState.myPlayerId;
  const myPlayer = props.gameState.gameState.getPlayerOf(myPlayerId)!;
  const currentRound = props.gameState.gameState.round;

  const onChangeBidDeclared = (changeBid: number) => {
    props.webSocketClient.sendBidDeclareChange(gameRoomId, changeBid);
  };

  if (props.gameState.changingPlayerId == myPlayerId) {
    return (
    <Box>
      <Grid container spacing={3}>
        <Typography variant='h4'>ビッドの変更が可能です</Typography>
        <TrickingPlayerBoard players={props.gameState.gameState.players}/>
        <Typography variant='h5'>ビッドの変更</Typography>
        <Grid container item xs={12} spacing={2}>
          {[...new Array(3).keys()].map(i => i - 1).map(bid => {
            if (myPlayer.declaredBid + bid <= currentRound && myPlayer.declaredBid + bid >= 0) {
              return <Grid item xs={1} key={bid}>
                <Button variant='contained' color='secondary' onClick={() => onChangeBidDeclared(bid)}>
                  {bid}
                </Button>
              </Grid>
            }

            return <Grid item xs={1}>
              <Button variant='contained' color='secondary' disabled>
                {bid}
              </Button>
            </Grid>;
          })}
        </Grid>
      </Grid>
    </Box>
    )
  }

  return <WaitingState
  gameState={props.gameState.gameState}
  waitFor="ビッド値の変更"
  />;
};

const NextTrickLeadPlayerChangingPhaseBoard = (props: { gameState: NextTrickLeadPlayerChangingPhase, webSocketClient: GameServerSocketClient }) => {
  const gameRoomId = props.gameState.gameState.gameRoomId;
  const myPlayerId = props.gameState.gameState.myPlayerId;

  const onChangeNextTrickLead = (newLeadPlayerId: string) => {
    props.webSocketClient.sendNextTrickLeadPlayerChange(gameRoomId, newLeadPlayerId);
  };

  if (props.gameState.changingPlayerId == myPlayerId) {
    return (
    <Box>
      <Grid container spacing={3}>
        <Typography variant='h4'>ディーラーの変更が可能です</Typography>
        <TrickingPlayerBoard players={props.gameState.gameState.players}/>
        <Typography variant='h5'>ディーラーの変更</Typography>
        <Grid container item xs={12} spacing={2}>
          {
            props.gameState.gameState.players.map(player => {
              return <Grid item key={player.playerId}>
                <Button variant='contained' color='secondary' onClick={() => onChangeNextTrickLead(player.playerId)}>
                  {player.playerId}
                </Button>
              </Grid>
            })
          }
        </Grid>
      </Grid>
    </Box>
    )
  }

  return <WaitingState
  gameState={props.gameState.gameState}
  waitFor="次のトリックのディーラー変更待ち"
  />;
};

const HandChangeWaitingPhaseBoard = (props: { gameState: HandChangeWaitingPhase, webSocketClient: GameServerSocketClient }) => {
  const gameRoomId = props.gameState.gameState.gameRoomId;
  const myPlayerId = props.gameState.gameState.myPlayerId;
  const [selectedCard, setSelectedCard] = useState<string[]>([]);

  const onSelectCard = (cardId: string) => {
    if (selectedCard.includes(cardId)) {
      setSelectedCard(old => old.filter(id => id != cardId));
    } else if (selectedCard.length < props.gameState.drawCardIds!.length) {
      setSelectedCard(old => [...old, cardId]);
    }
  }

  const onChangeHandCard = () => {
    props.webSocketClient.sendPlayerHandChange(gameRoomId, selectedCard);
  };

  if (props.gameState.changingPlayerId == myPlayerId) {
    return (
    <Box>
      <Grid container spacing={3}>
        <Typography variant='h4'>手札の変更が可能です</Typography>
        <TrickingPlayerBoard players={props.gameState.gameState.players}/>
        <Typography variant='h5'>手札の変更</Typography>
        <Grid container item xs={12} spacing={2}>
          {
            [...props.gameState.gameState.myCardIds, ...props.gameState.drawCardIds!].map(cardId => {
              return <Box>
                <ButtonBase onClick={() => onSelectCard(cardId)}>
                  <SkulkingCard cardId={cardId}/>
                </ButtonBase>
              </Box>
            })
          }
        </Grid>
        {selectedCard.length >= props.gameState.drawCardIds!.length &&
        <Grid container item xs={12}>
            <Button variant='contained' color='secondary'
                    onClick={onChangeHandCard}
            >
                手札を交換する
            </Button>
        </Grid>
        }
      </Grid>
    </Box>
    );
  }

  return <WaitingState
  gameState={props.gameState.gameState}
  waitFor="手札の変更"
  />;
};

const FuturePredicateWaitingPhaseBoard = (props: { gameState: FuturePredicateWaitingPhase, webSocketClient: GameServerSocketClient }) => {
  const gameRoomId = props.gameState.gameState.gameRoomId;
  const myPlayerId = props.gameState.gameState.myPlayerId;

  const onFinishFuturePredicate = () => {
    props.webSocketClient.sendFuturePredicateFinish(gameRoomId);
  };

  if (props.gameState.predicatingPlayerId == myPlayerId) {
    return (
    <Box>
      <Grid container spacing={3}>
        <Typography variant='h4'>現在の山札の閲覧が可能です</Typography>
        <TrickingPlayerBoard players={props.gameState.gameState.players}/>
        <Typography variant='h5'>山札の閲覧</Typography>
        <Grid container item xs={12} spacing={2}>
          {
            props.gameState.deckCards.map(cardId =>
            <Grid item><SkulkingCard cardId={cardId}/></Grid>
            )
          }
        </Grid>
        <Grid container item xs={12} spacing={2}>
          <Button
          color='secondary' variant='contained'
          onClick={onFinishFuturePredicate}
          >閲覧を終了する</Button>
        </Grid>
      </Grid>
    </Box>
    )
  }

  return <WaitingState
  gameState={props.gameState.gameState}
  waitFor="山札の閲覧終了待ち"
  />;
};

const TrickingPlayerBoard = (props: { players: TrickingPlayer[] }) => {
  return <Grid container item xs={12} spacing={2}>
    <Grid item xs={12}>
      <Typography variant='h5'>プレイヤー</Typography>
    </Grid>
    {props.players.map(player =>
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
}

const FinishedPhaseBoard = (props: { gameState: FinishedPhase, webSocketClient: GameServerSocketClient }) => {

  const gameRoomId = props.gameState.gameRoomId;

  const onCloseGame = () => {
    props.webSocketClient.sendEndGame(gameRoomId);
  }

  const onReplayGame = () => {
    props.webSocketClient.sendReplayGame(gameRoomId);
  }

  return (
  <Box>
    <Grid container spacing={3}>
      <Grid item xs={12}>
        <Typography variant='h4'>ゲーム終了</Typography>
      </Grid>
      <Grid item xs={12}>
        <Typography variant='h5'>スコア</Typography>
      </Grid>
      <Grid item xs={12}>
        <table>
          <thead>
          <th colSpan={2}>Player 1</th>
          <th colSpan={2}>Player 2</th>
          </thead>
          <tbody>
          {[...props.gameState.gameScore.flatMap(round =>
          [
            <tr>
              {[...round.keys()].flatMap(playerId => {
                const roundScore = round.get(playerId)!;
                return [
                  <td>{roundScore.score}</td>,
                  <td>{roundScore.bonus}</td>
                ]
              })}
            </tr>,
            <tr>
              {[...round.keys()].map(playerId => {
                const roundScore = round.get(playerId)!;
                return <td colSpan={2}>{roundScore.score + roundScore.bonus}</td>;
              })}
            </tr>
          ])]}
          <tr>
            <td colSpan={2}>total score</td>
            <td colSpan={2}>total score</td>
          </tr>
          </tbody>
        </table>
      </Grid>
      {props.gameState.roomOwnerId == props.gameState.myPlayerId &&
      <Grid container item xs={12} justify='center' spacing={2}>
          <Grid item>
              <Button variant='contained' color='secondary' onClick={onCloseGame}>
                  ゲームルームを終了する
              </Button>
          </Grid>
          <Grid item>
              <Button variant='contained' color='secondary' onClick={onReplayGame}>
                  再度プレイする
              </Button>
          </Grid>
      </Grid>
      }
    </Grid>
  </Box>
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