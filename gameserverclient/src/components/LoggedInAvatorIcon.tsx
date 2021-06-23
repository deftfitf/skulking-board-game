import React from "react";
import {Avatar, Box, ClickAwayListener, Grow, makeStyles, MenuItem, MenuList, Paper, Popper} from "@material-ui/core";
import {Link as RouterLink} from "react-router-dom";
import {gameServerApiClient} from "../module/axiousConfig";

export interface AvatorUser {
  username: string;
  icon: string;
}

interface AvatorIconProps {
  user: AvatorUser,
  onLogOut: () => void
}

const useStyles = makeStyles((theme) => ({
  avatorIcon: {
    width: theme.spacing(6),
    height: theme.spacing(6),
    cursor: "pointer",
  },
}));

const LoggedInAvatorIcon = (props: AvatorIconProps) => {
  const styles = useStyles();
  const [open, setOpen] = React.useState(false);
  const menuAnchorRef = React.useRef<HTMLDivElement>(null);

  const handleToggle = () => {
    setOpen((prevOpen) => !prevOpen);
  };

  const onLogOut: <T>(event: React.MouseEvent<T>) => void = (event) => {
    gameServerApiClient.logout()
    .then(() => props.onLogOut())
    .catch(e => console.log(`failed logout ${e}`));

    handleClose(event);
  }

  const handleClose: <T>(event: React.MouseEvent<T>) => void = (event) => {
    if (
    menuAnchorRef.current &&
    event.target instanceof Node &&
    menuAnchorRef.current.contains(event.target)
    ) {
      return;
    }

    setOpen(false);
  }

  const handleListKeyDown = (event: React.KeyboardEvent) => {
    if (event.key === 'Tab') {
      event.preventDefault();
      setOpen(false);
    }
  }

  const prevOpen = React.useRef(open);
  React.useEffect(() => {
    if (prevOpen.current && !open) {
      menuAnchorRef.current?.focus();
    }

    prevOpen.current = open;
  }, [open]);

  return (
  <Box>
    <Avatar
    ref={menuAnchorRef}
    alt={props.user.username}
    src={props.user.icon}
    className={styles.avatorIcon}
    onClick={handleToggle}
    variant="circle"
    />
    <Popper
    open={open}
    anchorEl={menuAnchorRef.current}
    role={undefined}
    transition disablePortal>
      {({TransitionProps, placement}) => (
      <Grow
      {...TransitionProps}
      style={{transformOrigin: placement === 'bottom' ? 'center top' : 'center bottom'}}
      >
        <Paper>
          <ClickAwayListener onClickAway={handleClose}>
            <MenuList autoFocusItem={open} id="menu-list-grow" onKeyDown={handleListKeyDown}>
              <MenuItem component={RouterLink} to="/players/mypage">Account</MenuItem>
              <MenuItem onClick={onLogOut}>Logout</MenuItem>
            </MenuList>
          </ClickAwayListener>
        </Paper>
      </Grow>
      )}
    </Popper>
  </Box>
  );
}

export default LoggedInAvatorIcon;