import React, {FormEvent, useState} from 'react';
import {Avatar, Button, Container, CssBaseline, makeStyles, TextField, Typography} from "@material-ui/core";
import LockOutlinedIcon from '@material-ui/icons/LockOutlined';
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

const SignIn = () => {
  const [playerId, setPlayerId] = useState<string | null>(null);
  const [password, setPassword] = useState<string | null>(null);
  const classes = useStyles();

  const tryLogin = (e: FormEvent<HTMLFormElement>) => {
    e.preventDefault();

    if (playerId && password) {
      gameServerApiClient
      .login(playerId, password)
      .then(() => window.location.assign('/'))
      .catch(e => console.error(e));
    }
  }

  return (
  <Container component="main" maxWidth="xs">
    <CssBaseline/>
    <div className={classes.paper}>
      <Avatar className={classes.avatar}>
        <LockOutlinedIcon/>
      </Avatar>
      <Typography component="h1" variant="h5">
        ログイン
      </Typography>
      <form className={classes.form} onSubmit={tryLogin} noValidate>
        <TextField
        variant="outlined"
        margin="normal"
        required
        fullWidth
        id="player-id"
        label="プレイヤーID"
        name="username"
        onChange={e => setPlayerId(e.target.value)}
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
          ログイン
        </Button>
      </form>
    </div>
  </Container>
  );
};

export default SignIn;