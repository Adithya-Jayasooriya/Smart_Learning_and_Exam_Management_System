import { createTheme } from "@mui/material/styles";

// Matches the Android app's indigo/teal Material palette.
export const theme = createTheme({
  palette: {
    primary: { main: "#3F51B5" },
    secondary: { main: "#00897B" },
    background: { default: "#f5f6fa" },
  },
  shape: { borderRadius: 10 },
});
