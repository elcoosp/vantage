import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
        plugins: [react()],
        server: {
                port: 5173,
                proxy: {
                        "/api": {
                                target: "http://localhost:8080",
                                changeOrigin: true,
                        },
                        // STOMP WebSocket for the Live Ops Map (raw WebSocket,
                        // not SockJS — hence ws: true).
                        "/ws": {
                                target: "http://localhost:8080",
                                ws: true,
                                changeOrigin: true,
                        },
                },
        },
});
