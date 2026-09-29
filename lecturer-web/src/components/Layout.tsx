import { type ReactNode } from "react";
import { Link, useLocation } from "react-router-dom";
import {
  AppBar,
  Box,
  Drawer,
  IconButton,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Toolbar,
  Typography,
} from "@mui/material";
import LogoutIcon from "@mui/icons-material/Logout";
import DashboardIcon from "@mui/icons-material/Dashboard";
import MenuBookIcon from "@mui/icons-material/MenuBook";
import UploadFileIcon from "@mui/icons-material/UploadFile";
import EditNoteIcon from "@mui/icons-material/EditNote";
import InboxIcon from "@mui/icons-material/Inbox";
import AssignmentIcon from "@mui/icons-material/Assignment";
import AssignmentTurnedInIcon from "@mui/icons-material/AssignmentTurnedIn";
import PersonIcon from "@mui/icons-material/Person";
import { useAuth } from "../auth/AuthContext";

const DRAWER_WIDTH = 240;

const NAV = [
  { to: "/", label: "Dashboard", icon: <DashboardIcon /> },
  { to: "/subjects", label: "Subjects", icon: <MenuBookIcon /> },
  { to: "/materials", label: "Materials", icon: <UploadFileIcon /> },
  { to: "/exams", label: "Exams", icon: <EditNoteIcon /> },
  { to: "/submissions", label: "Exam Submissions", icon: <InboxIcon /> },
  { to: "/assignments", label: "Assignments", icon: <AssignmentIcon /> },
  { to: "/assignment-submissions", label: "Assignment Subs", icon: <AssignmentTurnedInIcon /> },
  { to: "/profile", label: "Profile", icon: <PersonIcon /> },
];

export function Layout({ children }: { children: ReactNode }) {
  const { profile, logout } = useAuth();
  const location = useLocation();

  return (
    <Box sx={{ display: "flex" }}>
      <AppBar
        position="fixed"
        sx={{ zIndex: (t) => t.zIndex.drawer + 1 }}
      >
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            Smart Learning — Lecturer Portal
          </Typography>
          <Typography variant="body2" sx={{ mr: 2 }}>
            {profile?.fullName}
          </Typography>
          <IconButton color="inherit" onClick={() => void logout()} title="Log out">
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Drawer
        variant="permanent"
        sx={{
          width: DRAWER_WIDTH,
          flexShrink: 0,
          [`& .MuiDrawer-paper`]: { width: DRAWER_WIDTH, boxSizing: "border-box" },
        }}
      >
        <Toolbar />
        <Box sx={{ overflow: "auto" }}>
          <List>
            {NAV.map((item) => (
              <ListItemButton
                key={item.to}
                component={Link}
                to={item.to}
                selected={location.pathname === item.to}
              >
                <ListItemIcon>{item.icon}</ListItemIcon>
                <ListItemText primary={item.label} />
              </ListItemButton>
            ))}
          </List>
        </Box>
      </Drawer>

      <Box component="main" sx={{ flexGrow: 1, p: 3 }}>
        <Toolbar />
        {children}
      </Box>
    </Box>
  );
}
