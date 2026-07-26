import { MapView } from "./MapView";
import { useOpsMapSocket } from "./useOpsMapSocket";

export function OpsDashboard() {
	const { pins, isConnected } = useOpsMapSocket();

	return (
		<div className="relative w-full h-[calc(100vh-100px)] bg-gray-900 rounded-lg overflow-hidden">
			<MapView pins={pins} />

			{/* Counter */}
			<div className="absolute top-4 left-4 bg-black/70 text-white px-4 py-2 rounded-lg text-sm font-mono backdrop-blur-sm border border-gray-700">
				Live Shipments: <span className="font-bold text-red-400">{pins.length}</span>
			</div>

			{/* Reconnect badge */}
			{!isConnected && (
				<div className="absolute top-4 right-4 bg-red-600/90 text-white px-4 py-2 rounded-lg text-sm font-mono backdrop-blur-sm border border-red-400 animate-pulse">
					Reconnecting...
				</div>
			)}
		</div>
	);
}
