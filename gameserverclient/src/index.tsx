import React from 'react';
import ReactDOM from 'react-dom';
import './index.css';
import reportWebVitals from './reportWebVitals';
import {Box, Grid, makeStyles, Paper} from "@material-ui/core";

const useStyles = makeStyles((theme) => ({
  root: {
    flexGrow: 1,
  },
  paper: {
    padding: theme.spacing(1),
    textAlign: 'center',
    color: theme.palette.text.secondary,
  },
}));

const Demo = () => {
  const classes = useStyles();

  const FormRow = () => {
    return (
    <React.Fragment>
      <Grid item xs={4}>
        <Paper className={classes.paper}>item</Paper>
      </Grid>
      <Grid item xs={4}>
        <Paper className={classes.paper}>item</Paper>
      </Grid>
      <Grid item xs={4}>
        <Paper className={classes.paper}>item</Paper>
      </Grid>
    </React.Fragment>
    );
  }

  return (
  <Box className={classes.root}>
    <Grid container spacing={1}>
      <Grid container item xs={12} spacing={3}>
        <FormRow/>
      </Grid>
      <Grid container item xs={12} spacing={3}>
        <FormRow/>
      </Grid>
      <Grid container item xs={12} spacing={3}>
        <FormRow/>
      </Grid>
    </Grid>
  </Box>
  );
}

ReactDOM.render(
<React.StrictMode>
  <Demo/>
</React.StrictMode>,
document.getElementById('root')
);

// If you want to start measuring performance in your app, pass a function
// to log results (for example: reportWebVitals(console.log))
// or send to an analytics endpoint. Learn more: https://bit.ly/CRA-vitals
reportWebVitals();