import { useState } from "react";

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
		<form onSubmit={handleSubmit} className="flex items-center gap-2">
			<input
				type="number"
				value={quantity}
				onChange={(e) => setQuantity(Number(e.target.value))}
				className="w-20 px-2 py-1 border border-gray-300 rounded"
				disabled={isPending}
			/>
			<button
				type="submit"
				disabled={isPending}
				className="px-2 py-1 text-sm bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
			>
				Save
			</button>
			<button
				type="button"
				onClick={onCancel}
				disabled={isPending}
				className="px-2 py-1 text-sm bg-gray-300 rounded hover:bg-gray-400"
			>
				Cancel
			</button>
		</form>
	);
}
