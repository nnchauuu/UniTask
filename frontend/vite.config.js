import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, "..", "");

  return {
    plugins: [react()],
    envDir: "..",
    define: {
      "import.meta.env.VITE_GOOGLE_CLIENT_ID": JSON.stringify(
        env.GOOGLE_CLIENT_ID || process.env.GOOGLE_CLIENT_ID || ""
      )
    },
    server: {
      port: 5173
    }
  };
});
