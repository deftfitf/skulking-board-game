import React, {useEffect, useState} from 'react';
import {
  Box,
  Button,
  Container,
  makeStyles,
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

const GameRoomList = (props: { gamePlayer: GamePlayer }) => {
  const classes = useStyle();
  const history = useHistory();
  const pageLimit = 30;
  const [gameRooms, setGameRooms] = useState<GameRoom[]>([]);

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

  return (
  <section className={classes.root}>
    <Container>
      <Box className={classes.roomListBar}>
        <Typography variant="h4">GAME ROOMS</Typography>
        <Button variant="contained" color="primary">CREATE ROOM</Button>
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