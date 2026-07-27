import React from 'react';
import { useQuery } from '@tanstack/react-query';
import apiClient from '../../lib/api';
import { HeroBanner } from './components/HeroBanner';
import { ProductGrid } from './components/ProductGrid';
import { MarkdownText } from './components/MarkdownText';
// NOTE: To use this renderer, import it into your route and wrap with React Query provider.
// Example: <Route path="/storefront" element={<StorefrontRenderer />} />

interface ComponentDefinition {
  componentType: string;
  props: Record<string, any>;
}

async function fetchStorefrontLayout(): Promise<ComponentDefinition[]> {
  const response = await apiClient.get('/storefront');
  return response.data.components || [];
}

const componentMap: Record<string, React.ComponentType<any>> = {
  HeroBanner,
  ProductGrid,
  MarkdownText,
};

export function StorefrontRenderer() {
  const { data: components, isLoading, error } = useQuery({
    queryKey: ['storefront-layout'],
    queryFn: fetchStorefrontLayout,
    staleTime: 60000,
  });

  if (isLoading) {
    return <div className="p-4 text-gray-500">Loading storefront...</div>;
  }

  if (error) {
    return <div className="p-4 text-red-500">Failed to load storefront layout</div>;
  }

  if (!components || components.length === 0) {
    return <div className="p-4 text-gray-500">No components configured</div>;
  }

  return (
    <div className="storefront-renderer">
      {components.map((comp, index) => {
        const Component = componentMap[comp.componentType];
        if (!Component) {
          return (
            <div key={index} className="p-4 border border-yellow-300 bg-yellow-50 text-yellow-800">
              Unknown component type: {comp.componentType}
            </div>
          );
        }
        return <Component key={index} {...comp.props} />;
      })}
    </div>
  );
}
