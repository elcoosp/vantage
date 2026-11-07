import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { getToken, getTenantId } from "../../lib/auth";

export interface OpsMapPinPayload {
        orderId: string;
        lat: number;
        lon: number;
        productName: string;
        status?: string;
        timestamp: string;
}

/**
 * Subscribes to the tenant's live ops map over STOMP.
 *
 * - Token and tenant come from the auth store (see lib/auth.ts) — the old
 *   code read localStorage["token"], a key nothing ever wrote, so the socket
 *   never even attempted to connect.
 * - The broker URL is same-origin (`/ws`), so it works in dev through the
 *   Vite proxy and in production behind the same host, over ws:// or wss://.
 * - Pins arrive on /topic/ops-map.<tenantId>; the backend rejects
 *   subscriptions to other tenants' channels.
 */
export function useOpsMapSocket() {
        const [pins, setPins] = useState<OpsMapPinPayload[]>([]);
        const [isConnected, setIsConnected] = useState(false);

        useEffect(() => {
                const token = getToken();
                const tenantId = getTenantId();
                if (!token || !tenantId) {
                        console.warn("No auth session found, ops map WebSocket connection skipped");
                        return;
                }

                const wsProtocol = window.location.protocol === "https:" ? "wss" : "ws";
                const brokerURL = `${wsProtocol}://${window.location.host}/ws`;

                const client = new Client({
                        brokerURL,
                        connectHeaders: {
                                Authorization: `Bearer ${token}`,
                        },
                        reconnectDelay: 5000,
                        debug: (str) => {
                                console.debug("[STOMP]", str);
                        },
                        onConnect: () => {
                                console.log("STOMP connected");
                                setIsConnected(true);
                                client.subscribe(`/topic/ops-map.${tenantId}`, (message) => {
                                        try {
                                                const payload: OpsMapPinPayload = JSON.parse(message.body);
                                                setPins((prev) => [...prev.slice(-199), payload]);
                                        } catch (err) {
                                                console.error("Failed to parse OpsMapPinPayload", err);
                                        }
                                });
                        },
                        onDisconnect: () => {
                                console.log("STOMP disconnected");
                                setIsConnected(false);
                        },
                        onStompError: (frame) => {
                                console.error("STOMP error", frame);
                                setIsConnected(false);
                        },
                        onWebSocketClose: () => {
                                console.log("WebSocket closed");
                                setIsConnected(false);
                        },
                });

                client.activate();

                return () => {
                        client.deactivate();
                };
        }, []);

        return { pins, isConnected };
}
