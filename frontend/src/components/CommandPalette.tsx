import { Command } from "cmdk";
import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";

export function CommandPalette() {
	const [open, setOpen] = useState(false);
	const navigate = useNavigate();

	useEffect(() => {
		const down = (e: KeyboardEvent) => {
			if (e.key === "k" && (e.metaKey || e.ctrlKey)) {
				e.preventDefault();
				setOpen((prev) => !prev);
			}
		};
		document.addEventListener("keydown", down);
		return () => document.removeEventListener("keydown", down);
	}, []);

	const runCommand = (command: () => void) => {
		setOpen(false);
		command();
	};

	return (
		<Command.Dialog
			open={open}
			onOpenChange={setOpen}
			label="Global Command Palette"
			className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center"
		>
			<div className="w-full max-w-md bg-white dark:bg-gray-800 rounded-lg shadow-xl overflow-hidden">
				<Command.Input
					placeholder="Type a command or search…"
					className="w-full p-3 border-b border-gray-200 dark:border-gray-700 outline-none bg-transparent"
				/>
				<Command.List className="max-h-72 overflow-y-auto p-2">
					<Command.Empty>No results found.</Command.Empty>
					<Command.Group heading="Navigation">
						<Command.Item onSelect={() => runCommand(() => navigate("/"))}>Go to Dashboard</Command.Item>
						<Command.Item onSelect={() => runCommand(() => navigate("/inventory"))}>Go to Inventory</Command.Item>
						<Command.Item onSelect={() => runCommand(() => navigate("/products"))}>Go to Products</Command.Item>
					</Command.Group>
					<Command.Group heading="Actions">
						<Command.Item onSelect={() => runCommand(() => alert("Add New Product"))}>Add New Product</Command.Item>
						<Command.Item onSelect={() => runCommand(() => alert("Update Stock"))}>Update Stock</Command.Item>
					</Command.Group>
				</Command.List>
			</div>
		</Command.Dialog>
	);
}
