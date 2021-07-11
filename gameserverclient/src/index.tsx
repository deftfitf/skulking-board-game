import React, {useEffect, useState} from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import reportWebVitals from './reportWebVitals';
import {Button, Container, makeStyles, Typography} from "@material-ui/core";
import AppTopBar from "./components/AppTopBar";
import {HashRouter, Link as RouterLink, Route, Switch, useHistory} from "react-router-dom";
import GameRoomList from "./pages/GameRoomList";
import GameRoom from "./pages/GameRoom";
import SignIn from "./pages/SignIn";
import SignUp from "./pages/SignUp";
import PlayerMyPage from "./pages/PlayerMyPage";
import {gameServerApiClient} from "./module/axiousConfig";
import {GamePlayer} from "./models/Models";

const useStyles = makeStyles((theme) => ({
  root: {
    position: 'relative',
    display: 'flex',
    alignItems: 'center',
    [theme.breakpoints.up('sm')]: {
      height: '80vh',
      minHeight: 500,
      maxHeight: 1300,
    },
  },
  container: {
    marginTop: theme.spacing(3),
    marginBottom: theme.spacing(14),
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
  },
  paper: {
    padding: theme.spacing(1),
    textAlign: 'center',
    color: theme.palette.text.secondary,
  },
  button: {
    minWidth: 200,
  },
  h5: {
    marginBottom: theme.spacing(4),
    marginTop: theme.spacing(4),
    [theme.breakpoints.up('sm')]: {
      marginTop: theme.spacing(10),
    },
  },
  more: {
    marginTop: theme.spacing(2),
  },
}));

const TopPage = () => {
  const classes = useStyles();

  return (
  <React.Fragment>
    <Typography color="inherit" align="center" variant="h2">
      SKULKING ONLINE
    </Typography>
    <Button
    color="secondary"
    variant="contained"
    size="large"
    className={classes.button}
    component={RouterLink}
    to="/signup"
    >
      新規登録
    </Button>
  </React.Fragment>
  )
}

const App = () => {
  const classes = useStyles();
  const history = useHistory();
  const [player, setPlayer] = useState<GamePlayer | null>(null);

  const routes = [
    {
      path: "/gamerooms/new",
      render: () => {
        if (player) {
          return <GameRoom gamePlayer={player}/>;
        } else {
          return <TopPage/>
        }
      }
    },
    {
      path: "/gamerooms/:gameRoomId",
      render: () => {
        if (player) {
          return <GameRoom gamePlayer={player}/>;
        } else {
          return <TopPage/>
        }
      }
    },
    {
      path: "/players/mypage",
      component: PlayerMyPage
    },
    {
      path: "/signin",
      component: SignIn
    },
    {
      path: "/signup",
      component: SignUp
    },
    {
      path: "/",
      render: () => {
        if (player) {
          return <GameRoomList gamePlayer={player}/>;
        } else {
          return <TopPage/>
        }
      }
    },
  ];

  useEffect(() => {
    gameServerApiClient
    .checkLogin()
    .then(player => {
      setPlayer(player);
    })
    .catch(() => {
      setPlayer(null);
    });

    return () => {
    };
  }, []);

  return (
  <HashRouter>
    <AppTopBar
    auth={player !== null}
    onLogOut={() => setPlayer(null)}
    user={{
      username: player?.displayName ?? 'unknown',
      icon: player?.iconUrl ?? 'unknown'
    }}/>

    <section className={classes.root}>
      <Container className={classes.container}>
        <Switch>
          {routes.map(route => (
          <Route {...route}/>
          ))}
        </Switch>
      </Container>
    </section>
  </HashRouter>
  );
}

ReactDOM.render(
<React.StrictMode>
  <App/>
</React.StrictMode>,
document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();