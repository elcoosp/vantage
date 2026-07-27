import React from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../../lib/api';

interface Product {
  id: string;
  name: string;
  price: number;
  description?: string;
}

async function fetchProducts(limit: number): Promise<Product[]> {
  try {
    const response = await apiClient.get('/products', { params: { limit } });
    return response.data;
  } catch (error) {
    console.warn('Product API not available, using mock data for demo');
    // Mock data fallback for demonstration
    return Array.from({ length: limit }, (_, i) => ({
      id: `mock-${i}`,
      name: `Mock Product ${i+1}`,
      price: 19.99 + i * 5,
      description: 'This is a mock product for demonstration'
    }));
  }
}

interface ProductGridProps {
  title: string;
  limit: number;
}

export function ProductGrid({ title, limit }: ProductGridProps) {
  const { data: products, isLoading, error } = useQuery({
    queryKey: ['storefront-products', limit],
    queryFn: () => fetchProducts(limit),
  });

  if (isLoading) return <div className="p-4 text-gray-500">Loading products...</div>;
  if (error) return <div className="p-4 text-red-500">Failed to load products</div>;
  if (!products || products.length === 0) return <div className="p-4 text-gray-500">No products available</div>;

  return (
    <div className="p-4">
      <h2 className="text-2xl font-semibold mb-4">{title}</h2>
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        {products.map((product) => (
          <div key={product.id} className="border rounded-lg p-4 shadow-sm hover:shadow-md transition">
            <h3 className="font-medium">{product.name}</h3>
            <p className="text-gray-600">${product.price.toFixed(2)}</p>
            {product.description && <p className="text-sm text-gray-500">{product.description}</p>}
          </div>
        ))}
      </div>
    </div>
  );
}
