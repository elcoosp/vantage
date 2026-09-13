import { useEffect, useRef, useState } from "react";

interface Props {
	currentQuantity: number;
	onSubmit: (quantity: number) => void;
	onCancel: () => void;
	isPending: boolean;
}

export function InventoryEditForm({ currentQuantity, onSubmit, onCancel, isPending }: Props) {
	const [quantity, setQuantity] = useState(currentQuantity);
	const inputRef = useRef<HTMLInputElement>(null);
	const committedRef = useRef(false);

	useEffect(() => {
		const input = inputRef.current;
		if (!input) return;
		input.focus();
		input.select();
	}, []);

	const commit = () => {
		if (committedRef.current) return;
		committedRef.current = true;
		const next = Number.isFinite(quantity) ? Math.max(0, Math.round(quantity)) : currentQuantity;
		if (next === currentQuantity) {
			onCancel();
			return;
		}
		onSubmit(next);
	};

	const cancel = () => {
		committedRef.current = true;
		onCancel();
	};

	return (
		<input
			ref={inputRef}
			type="number"
			min={0}
			inputMode="numeric"
			value={quantity}
			disabled={isPending}
			onChange={(e) => setQuantity(Number(e.target.value))}
			onKeyDown={(e) => {
				if (e.key === "Enter") {
					e.preventDefault();
					commit();
				} else if (e.key === "Escape") {
					e.preventDefault();
					cancel();
				}
			}}
			onBlur={commit}
			aria-label="Edit on-hand quantity"
			className="h-7 w-full rounded-md border border-brand-500 bg-card-light px-2 text-right text-[13px] font-semibold tabular-nums text-ink-light outline-none ring-2 ring-brand-500/20 dark:border-brand-400 dark:bg-card-dark dark:text-ink-dark"
		/>
	);
}
