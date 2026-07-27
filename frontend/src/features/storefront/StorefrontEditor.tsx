import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import apiClient from '../../lib/api';
import toast from 'react-hot-toast';

interface ComponentDefinition {
  componentType: string;
  props: Record<string, any>;
}

async function fetchStorefrontLayout(): Promise<ComponentDefinition[]> {
  const response = await apiClient.get('/storefront');
  return response.data.components || [];
}

async function updateStorefrontLayout(components: ComponentDefinition[]): Promise<void> {
  await apiClient.put('/storefront', components);
}

const COMPONENT_TYPES = ['HeroBanner', 'ProductGrid', 'MarkdownText'];

export function StorefrontEditor() {
  const queryClient = useQueryClient();
  const { data: components, isLoading } = useQuery({
    queryKey: ['storefront-layout'],
    queryFn: fetchStorefrontLayout,
  });

  const [editingComponents, setEditingComponents] = useState<ComponentDefinition[]>([]);

  React.useEffect(() => {
    if (components) {
      setEditingComponents(components);
    }
  }, [components]);

  const mutation = useMutation({
    mutationFn: updateStorefrontLayout,
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['storefront-layout'] });
      toast.success('Layout updated successfully');
    },
    onError: () => {
      toast.error('Failed to update layout');
    },
  });

  const addComponent = () => {
    setEditingComponents([
      ...editingComponents,
      { componentType: 'HeroBanner', props: { title: 'New Component', imageUrl: '' } },
    ]);
  };

  const removeComponent = (index: number) => {
    setEditingComponents(editingComponents.filter((_, i) => i !== index));
  };

  const moveComponent = (index: number, direction: 'up' | 'down') => {
    const newIndex = direction === 'up' ? index - 1 : index + 1;
    if (newIndex < 0 || newIndex >= editingComponents.length) return;
    const newComponents = [...editingComponents];
    [newComponents[index], newComponents[newIndex]] = [newComponents[newIndex], newComponents[index]];
    setEditingComponents(newComponents);
  };

  const updateComponentType = (index: number, type: string) => {
    const newComponents = [...editingComponents];
    newComponents[index].componentType = type;
    setEditingComponents(newComponents);
  };

  const updateProp = (index: number, key: string, value: any) => {
    const newComponents = [...editingComponents];
    newComponents[index].props[key] = value;
    setEditingComponents(newComponents);
  };

  const handleSave = () => {
    mutation.mutate(editingComponents);
  };

  if (isLoading) return <div>Loading editor...</div>;

  return (
    <div className="p-4 max-w-4xl mx-auto">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-2xl font-bold">Storefront Editor</h2>
        <div className="space-x-2">
          <button
            onClick={addComponent}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            Add Component
          </button>
          <button
            onClick={handleSave}
            disabled={mutation.isPending}
            className="px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700 disabled:opacity-50"
          >
            {mutation.isPending ? 'Saving...' : 'Save Layout'}
          </button>
        </div>
      </div>

      <div className="space-y-4">
        {editingComponents.map((comp, index) => (
          <div key={index} className="border rounded p-4 bg-white shadow-sm">
            <div className="flex justify-between items-start">
              <div className="flex-1 space-y-2">
                <div>
                  <label className="block text-sm font-medium">Component Type</label>
                  <select
                    value={comp.componentType}
                    onChange={(e) => updateComponentType(index, e.target.value)}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded"
                  >
                    {COMPONENT_TYPES.map((type) => (
                      <option key={type} value={type}>{type}</option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="block text-sm font-medium">Props (JSON)</label>
                  <textarea
                    value={JSON.stringify(comp.props, null, 2)}
                    onChange={(e) => {
                      try {
                        const props = JSON.parse(e.target.value);
                        updateProp(index, 'props', props);
                      } catch (err) {
                        // ignore invalid JSON
                      }
                    }}
                    className="mt-1 block w-full px-3 py-2 border border-gray-300 rounded font-mono text-sm"
                    rows={3}
                  />
                </div>
              </div>
              <div className="flex flex-col space-y-1 ml-4">
                <button
                  onClick={() => moveComponent(index, 'up')}
                  disabled={index === 0}
                  className="px-2 py-1 bg-gray-200 rounded hover:bg-gray-300 disabled:opacity-50"
                >
                  ↑
                </button>
                <button
                  onClick={() => moveComponent(index, 'down')}
                  disabled={index === editingComponents.length - 1}
                  className="px-2 py-1 bg-gray-200 rounded hover:bg-gray-300 disabled:opacity-50"
                >
                  ↓
                </button>
                <button
                  onClick={() => removeComponent(index)}
                  className="px-2 py-1 bg-red-500 text-white rounded hover:bg-red-600"
                >
                  ✕
                </button>
              </div>
            </div>
          </div>
        ))}
        {editingComponents.length === 0 && (
          <div className="text-center text-gray-500 py-8">No components. Click "Add Component" to start.</div>
        )}
      </div>
    </div>
  );
}
