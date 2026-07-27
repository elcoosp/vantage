import { useQuery } from "@tanstack/react-query";
import axios from "axios";
import { OrderResponse } from "../../api/generated/models/OrderResponse";

export interface Order extends OrderResponse {
  productName: string;
  quantity: number;
  createdAt: string;
}

export interface OrderSearchResponse {
  content: Order[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
}

async function fetchOrders(): Promise<OrderSearchResponse> {
  const response = await axios.get<OrderSearchResponse>("/api/v1/orders/search", {
    params: {
      size: 10000,
    },
  });
  return response.data;
}

export function useOrders() {
  return useQuery({
    queryKey: ["orders", 10000],
    queryFn: fetchOrders,
    staleTime: 1000 * 60 * 5,
  });
}
