import React, {useEffect, useState} from 'react';
import {
  Box,
  Button,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  FormControl,
  InputLabel,
  makeStyles,
  MenuItem,
  Select,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableRow,
  Typography
} from "@material-ui/core";
import {GamePlayer, GameRoom, GetGameRoomsRequest} from "../models/Models";
import {gameServerApiClient} from "../module/axiousConfig";
import {useHistory} from "react-router-dom";

const useStyle = makeStyles((theme) => ({
  root: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  },
  roomListBar: {
    display: 'flex',
    justifyContent: 'space-between',
    marginBottom: theme.spacing(3),
  },
  gameRoomPagination: {
    marginTop: theme.spacing(3),
  },
  gameRoomPaginationLeftButton: {
    float: 'left'
  },
  gameRoomPaginationRightButton: {
    float: 'right'
  },
}));

type PaginationProps = {
  lastExclusiveStartKey?: string;
  nextExclusiveStartKey?: string;
  loadNextRooms: (exclusiveStartKey?: string) => void;
}

const GameRoomPagination = (props: PaginationProps) => {
  const classes = useStyle();

  return (
  <Box className={classes.gameRoomPagination}>
    {props.lastExclusiveStartKey &&
    <Button
        className={classes.gameRoomPaginationLeftButton}
        variant="text"
        color="primary"
        onClick={() => props.loadNextRooms()}
    >
        最初のページ
    </Button>
    }
    {props.nextExclusiveStartKey &&
    <Button
        className={classes.gameRoomPaginationRightButton}
        variant="text"
        color="primary"
        onClick={() => props.loadNextRooms(props.nextExclusiveStartKey)}
    >
        次のページ
    </Button>
    }
  </Box>
  )
}

const GameRoomCreateDialog = (props: { open: boolean, setOpen: (p: boolean) => void }) => {
  const history = useHistory();
  const [roomSize, setRoomSize] = useState(4);
  const [nOfRounds, setNOfRounds] = useState(5);
  const [deckType, setDeckType] = useState<"STANDARD" | "EXPANSION">("STANDARD");

  const createRoom = () => {
    history.push({
      pathname: `/gamerooms/create`,
      state: {
        createRequest: {
          roomSize: roomSize,
          nOfRounds: nOfRounds,
          deckType: deckType,
        },
        isReConnect: false
      }
    })
  }

  const createRoomDialogClose = () => {
    props.setOpen(false);
  };

  return <Dialog open={props.open} onClose={createRoomDialogClose} aria-labelledby="form-dialog-title">
    <DialogTitle id="form-dialog-title">新規ゲームルーム作成</DialogTitle>
    <DialogContent>
      <DialogContentText>
        新規にゲームルームを作成します.
      </DialogContentText>
      <FormControl>
        <InputLabel id="create-room-deck-type-label">デッキタイプ</InputLabel>
        <Select
        labelId="create-room-deck-type-label"
        id="create-room-deck-type-select"
        value={deckType}
        onChange={e => setDeckType(e.target.value as ("STANDARD" | "EXPANSION"))}
        >
          <MenuItem value={"STANDARD"}>標準</MenuItem>
          <MenuItem value={"EXPANSION"}>エクスパンジョン</MenuItem>
        </Select>
      </FormControl>
      <br/>
      <FormControl>
        <InputLabel id="create-room-room-size-label">ルーム人数</InputLabel>
        <Select
        labelId="create-room-room-size-label"
        id="create-room-room-size-select"
        value={roomSize}
        onChange={e => setRoomSize(e.target.value as number)}
        >
          {[...new Array(5).keys()]
          .map(i => <MenuItem value={i + 2}>{i + 2}</MenuItem>)}
        </Select>
      </FormControl>
      <br/>
      <FormControl>
        <InputLabel id="create-room-n-of-round-label">ラウンド数</InputLabel>
        <Select
        labelId="create-room-n-of-round-label"
        id="create-room-n-of-round-select"
        value={nOfRounds}
        onChange={e => setNOfRounds(e.target.value as number)}
        >
          {[...new Array(10).keys()]
          .map(i => <MenuItem value={i + 1}>{i + 1}</MenuItem>)}
        </Select>
      </FormControl>
    </DialogContent>
    <DialogActions>
      <Button onClick={createRoomDialogClose} color="primary">
        キャンセル
      </Button>
      <Button onClick={createRoom} color="primary">
        作成
      </Button>
    </DialogActions>
  </Dialog>;
}

const GameRoomList = (props: { gamePlayer: GamePlayer }) => {
  const classes = useStyle();
  const history = useHistory();
  const pageLimit = 30;
  const [gameRooms, setGameRooms] = useState<GameRoom[]>([]);
  const [dialogOpen, setDialogOpen] = useState(false);

  useEffect(() => {
    loadRooms();
  }, []);

  const loadRooms = (exclusiveStartKey?: string) => {
    const request: GetGameRoomsRequest = {
      limit: pageLimit,
      exclusiveStartKey: exclusiveStartKey
    }
    gameServerApiClient.getGameRooms(request)
    .then(_gameRooms => setGameRooms(_gameRooms))
    .catch(e => console.error(e));
  }

  const beforeKey = (key: string) =>
  key.slice(0, -1) + String.fromCharCode((key.charCodeAt(key.length - 1) - 1))

  // TODO: Game Room作成時の接続の挙動に問題があるので修正する
  return (
  <section className={classes.root}>
    <Container>
      <Box className={classes.roomListBar}>
        <Typography variant="h4">GAME ROOMS</Typography>
        <div>
          <Button variant="contained" color="primary" onClick={() => setDialogOpen(true)}>CREATE ROOM</Button>
          <GameRoomCreateDialog open={dialogOpen} setOpen={setDialogOpen}/>
        </div>
      </Box>
      <Table size="small">
        <TableHead>
          <TableRow>
            <TableCell>ルームID</TableCell>
            <TableCell>オーナー</TableCell>
            <TableCell>状態</TableCell>
            <TableCell>参加者</TableCell>
            <TableCell>参加</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {gameRooms.map((gameRoom) => (
          <TableRow key={gameRoom.gameRoomId}>
            <TableCell>{gameRoom.gameRoomId}</TableCell>
            <TableCell>{gameRoom.roomOwnerId}</TableCell>
            <TableCell>{gameRoom.gameState}</TableCell>
            <TableCell>{gameRoom.joinedPlayerIds.join(",")}</TableCell>
            <TableCell>
              <Button
              variant="outlined"
              color="primary"
              onClick={() => history.push({
                pathname: `/gamerooms/${gameRoom.gameRoomId}`,
                state: {
                  playerId: props.gamePlayer.playerId,
                  isReConnect: gameRoom.joinedPlayerIds.includes(props.gamePlayer.playerId)
                }
              })}
              >
                JOIN
              </Button>
            </TableCell>
          </TableRow>
          ))}
        </TableBody>
      </Table>
      <GameRoomPagination
      lastExclusiveStartKey={gameRooms.length > 0 ? beforeKey(gameRooms[0].gameRoomId) : undefined}
      nextExclusiveStartKey={gameRooms.length >= pageLimit ? gameRooms[gameRooms.length - 1].gameRoomId : undefined}
      loadNextRooms={loadRooms}/>
    </Container>
  </section>
  );
};

export default GameRoomList;