import React from 'react';
import {AppBar, Box, createMuiTheme, Link, makeStyles, Toolbar} from "@material-ui/core";
import clsx from "clsx";
import {purple} from "@material-ui/core/colors";
import LoggedInAvatorIcon, {AvatorUser} from "./LoggedInAvatorIcon";
import {Link as RouterLink} from "react-router-dom";

const useStyles = makeStyles((theme) => ({
  title: {
    fontSize: 24,
  },
  toolbar: {
    justifyContent: 'space-between',
  },
  left: {
    flex: 1,
  },
  right: {
    flex: 1,
    display: 'flex',
    justifyContent: 'flex-end',
  },
  rightLink: {
    fontSize: 16,
    color: theme.palette.common.white,
    marginLeft: theme.spacing(3),
  },
  linkSecondary: {
    color: theme.palette.secondary.main,
  },
}));

// styled component
// const CustomAppBar = styled(AppBar)({
//
// })

// Box Component
// marginを意味する,
// その設定名称と実際のお設定値との紐付け情報はテーマで持っている
// @material-ui/core/stylesのuseThemeで、そのテーマ情報にアクセスし使用できます

// createMuiThemeを使った書き方ができる
// ルート階層にて, ThemeProviderコンポーネントでアプリのコンポーネントをラップすることで
// 下層のコンポーネントにテーマ情報を渡す
// 以下はテーマのパレットからーを上書きしている例になる

const theme = createMuiTheme({
  palette: {
    primary: purple,
  },
});

interface AppTopBarProps {
  auth: boolean;
  user?: AvatorUser;
  onLogOut: () => void;
}

const AppTopBar = (props: AppTopBarProps) => {
  const styles = useStyles();

  let ToolBarRight: JSX.Element;
  if (props.auth && props.user) {
    ToolBarRight = <React.Fragment>
      <Link
      variant="h6"
      underline="none"
      component={RouterLink}
      to="/gamerooms/"
      >
        {'GAME ROOMS'}
      </Link>
      <LoggedInAvatorIcon user={props.user} onLogOut={props.onLogOut}/>
    </React.Fragment>
  } else {
    ToolBarRight = <React.Fragment>
      <Link
      variant="h6"
      underline="none"
      className={styles.rightLink}
      component={RouterLink}
      to="/signin"
      >
        {'ログイン'}
      </Link>
      <Link
      variant="h6"
      underline="none"
      className={clsx(styles.rightLink, styles.linkSecondary)}
      component={RouterLink}
      to="/signup"
      >
        {'新規登録'}
      </Link>
    </React.Fragment>
  }

  return (
  <React.Fragment>
    <AppBar position="fixed">
      <Toolbar className={styles.toolbar}>
        <Box>
          <Link
          variant="h6"
          underline="none"
          color="inherit"
          className={styles.title}
          component={RouterLink}
          to="/"
          >
            {'SKULKING ONLINE'}
          </Link>
        </Box>
        <Box className={styles.right}>
          {ToolBarRight}
        </Box>
      </Toolbar>
    </AppBar>
  </React.Fragment>
  );
}
;

export default AppTopBar;

