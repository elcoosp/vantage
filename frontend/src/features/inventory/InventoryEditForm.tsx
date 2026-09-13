import { useState } from "react";
import { Button, Input } from "../../components/ui";

interface Props {
	currentQuantity: number;
	onSubmit: (quantity: number) => void;
	onCancel: () => void;
	isPending: boolean;
}

export function InventoryEditForm({ currentQuantity, onSubmit, onCancel, isPending }: Props) {
	const [quantity, setQuantity] = useState(currentQuantity);

	const handleSubmit = (e: React.FormEvent) => {
		e.preventDefault();
		onSubmit(quantity);
	};

	return (
		<form onSubmit={handleSubmit} className="inline-flex items-center justify-end gap-2">
			<Input
				type="number"
				value={quantity}
				min={0}
				onChange={(e) => setQuantity(Number(e.target.value))}
				className="h-7 w-20 text-right"
				disabled={isPending}
				aria-label="New on-hand quantity"
			/>
			<Button type="submit" size="sm" disabled={isPending}>
				Save
			</Button>
			<Button type="button" variant="secondary" size="sm" onClick={onCancel} disabled={isPending}>
				Cancel
			</Button>
		</form>
	);
}
