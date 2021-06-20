import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import reportWebVitals from './reportWebVitals';
import {Button, Container, makeStyles, Typography} from "@material-ui/core";
import AppTopBar from "./components/AppTopBar";

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
  <section className={classes.root}>
    <Container className={classes.container}>
      <Typography color="inherit" align="center" variant="h2">
        SKULKING ONLINE
      </Typography>
      <Button
      color="secondary"
      variant="contained"
      size="large"
      className={classes.button}
      component="a"
      href="/register"
      >
        新規登録
      </Button>
    </Container>
  </section>
  )
}

ReactDOM.render(
<React.StrictMode>
  <AppTopBar auth={false} user={{username: "ishida", icon: "https://material-ui.com/static/images/avatar/1.jpg"}}/>
  <TopPage/>
</React.StrictMode>,
document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();