import { create } from "zustand";

interface UIState {
	addProductModalOpen: boolean;
	updateStockModalOpen: boolean;
	openAddProductModal: () => void;
	closeAddProductModal: () => void;
	openUpdateStockModal: () => void;
	closeUpdateStockModal: () => void;
}

export const useUIStore = create<UIState>((set) => ({
	addProductModalOpen: false,
	updateStockModalOpen: false,
	openAddProductModal: () => set({ addProductModalOpen: true }),
	closeAddProductModal: () => set({ addProductModalOpen: false }),
	openUpdateStockModal: () => set({ updateStockModalOpen: true }),
	closeUpdateStockModal: () => set({ updateStockModalOpen: false }),
}));
