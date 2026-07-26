import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";

export interface OpsMapPinPayload {
	orderId: string;
	lat: number;
	lon: number;
	productName: string;
	timestamp: string;
}

export function useOpsMapSocket() {
	const [pins, setPins] = useState<OpsMapPinPayload[]>([]);
	const [isConnected, setIsConnected] = useState(false);

	useEffect(() => {
		const token = localStorage.getItem("token");
		if (!token) {
			console.warn("No JWT token found, WebSocket connection skipped");
			return;
		}

		const client = new Client({
			brokerURL: "ws://localhost:8080/ws",
			connectHeaders: {
				Authorization: `Bearer ${token}`,
			},
			debug: (str) => {
				console.debug("[STOMP]", str);
			},
			onConnect: () => {
				console.log("STOMP connected");
				setIsConnected(true);
				client.subscribe("/topic/ops-map", (message) => {
					try {
						const payload: OpsMapPinPayload = JSON.parse(message.body);
						setPins((prev) => [...prev, payload]);
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
