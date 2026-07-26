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
					fillColor="#ff4136"
					color="#ff4136"
					weight={2}
					opacity={1}
					fillOpacity={0.8}
					className="pulse-marker"
				>
					<Popup>
						<strong>{pin.productName}</strong>
						<br />
						Order: {pin.orderId}
						<br />
						{new Date(pin.timestamp).toLocaleTimeString()}
					</Popup>
				</CircleMarker>
			))}
		</MapContainer>
	);
}
