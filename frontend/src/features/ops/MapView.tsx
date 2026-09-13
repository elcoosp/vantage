import { CircleMarker, MapContainer, Popup, TileLayer } from "react-leaflet";
import type { OpsMapPinPayload } from "./useOpsMapSocket";

interface MapViewProps {
	pins: OpsMapPinPayload[];
}

export function MapView({ pins }: MapViewProps) {
	return (
		<MapContainer center={[20, 0]} zoom={2} style={{ height: "100%", width: "100%" }} zoomControl={false}>
			<TileLayer
				attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors &copy; <a href="https://carto.com/">CARTO</a>'
				url="https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
			/>
			{pins.map((pin) => (
				<CircleMarker
					key={pin.orderId}
					center={[pin.lat, pin.lon]}
					radius={8}
					fillColor="#5257CE"
					color="#5257CE"
					weight={2}
					opacity={1}
					fillOpacity={0.8}
					className="pulse-marker"
				>
					<Popup>
						<div className="space-y-0.5 text-[12px]">
							<strong className="text-ink-light dark:text-ink-dark">{pin.productName}</strong>
							<br />
							<span className="text-ink3-light dark:text-ink3-dark">Order: </span>
							<span className="font-mono text-ink2-light dark:text-ink2-dark">{pin.orderId}</span>
							<br />
							<span className="text-ink3-light dark:text-ink3-dark">
								{new Date(pin.timestamp).toLocaleTimeString()}
							</span>
						</div>
					</Popup>
				</CircleMarker>
			))}
		</MapContainer>
	);
}
