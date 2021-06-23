import React from 'react';
import {Container, makeStyles, Typography} from "@material-ui/core";

const useStyle = makeStyles((theme) => ({
  root: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center'
  }
}));

const GameRoomList = () => {
  const classes = useStyle();

  return (
  <section className={classes.root}>
    <Container>
      <Typography color="inherit" align="center" variant="h2">
        Game Rooms
      </Typography>
      
    </Container>
  </section>
  );
};

export default GameRoomList;