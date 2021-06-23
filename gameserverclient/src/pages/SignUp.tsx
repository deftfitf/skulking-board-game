import React, {FormEvent, useState} from 'react';
import {Button, Container, CssBaseline, makeStyles, TextField, Typography} from "@material-ui/core";
import {useHistory} from "react-router-dom";
import {gameServerApiClient} from "../module/axiousConfig";

const useStyles = makeStyles((theme) => ({
  paper: {
    marginTop: theme.spacing(8),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  avatar: {
    margin: theme.spacing(1),
    backgroundColor: theme.palette.secondary.main,
  },
  form: {
    width: '100%', // Fix IE 11 issue.
    marginTop: theme.spacing(1),
  },
  submit: {
    margin: theme.spacing(3, 0, 2),
  },
}));

const SignUp = () => {
  const [playerId, setPlayerId] = useState<string | null>(null);
  const [playerName, setPlayerName] = useState<string | null>(null);
  const [password, setPassword] = useState<string | null>(null);
  const history = useHistory();
  const classes = useStyles();

  const tryRegister = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (playerName && password) {
      gameServerApiClient
      .register({
        playerName: playerName,
        playerPassword: password
      })
      .then(registered => {
        setPlayerId(registered.playerId)
      })
      .catch(e => console.error(e));
    }
  }

  return (
  <Container component="main" maxWidth="xs">
    <CssBaseline/>
    <div className={classes.paper}>
      {playerId &&
      <React.Fragment>
          <Typography variant="h6">プレイヤー登録完了</Typography>
          <p>プレイヤーID: {playerId}</p>
      </React.Fragment>
      }
      <Typography component="h1" variant="h5">
        新規登録
      </Typography>
      <form className={classes.form} onSubmit={tryRegister} noValidate>
        <TextField
        variant="outlined"
        margin="normal"
        required
        fullWidth
        id="player-id"
        label="プレイヤー名"
        name="playerName"
        onChange={e => setPlayerName(e.target.value)}
        autoFocus
        />
        <TextField
        variant="outlined"
        margin="normal"
        required
        fullWidth
        name="password"
        label="パスワード"
        type="password"
        id="password"
        onChange={e => setPassword(e.target.value)}
        autoComplete="current-password"
        />
        <Button
        type="submit"
        fullWidth
        variant="contained"
        color="primary"
        className={classes.submit}
        >
          新規登録
        </Button>
      </form>
    </div>
  </Container>
  );
}
;

export default SignUp;