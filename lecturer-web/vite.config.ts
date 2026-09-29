import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    open: true,
  },
  build: {
    // Increase the warning limit to prevent the warning (e.g., to 1000 kB)
    chunkSizeWarningLimit: 1000, 
  },
});
