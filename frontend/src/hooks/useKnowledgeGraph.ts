import { useState, useCallback } from 'react';
import {
  Node,
  Edge,
  Position,
  applyNodeChanges,
  applyEdgeChanges,
  OnNodesChange,
  OnEdgesChange,
  NodeChange,
  EdgeChange,
} from 'reactflow';

const initialNodes: Node[] = [
  {
    id: '101',
    type: 'input',
    data: { label: '函数概念' },
    position: { x: 50, y: 150 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: {
      background: '#3b82f6',
      color: 'white',
      border: 'none',
      borderRadius: '8px',
      width: 140,
      padding: '12px',
      fontSize: '16px',
      fontWeight: 'bold',
      textAlign: 'center',
      boxShadow: '0 4px 6px -1px rgb(0 0 0 / 0.1)',
    },
  },
  {
    id: '201',
    data: { label: '一次函数' },
    position: { x: 350, y: 50 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: {
      background: '#ffffff',
      color: '#1e293b',
      border: '1px solid #94a3b8',
      borderRadius: '8px',
      width: 140,
      padding: '10px',
      fontSize: '14px',
      textAlign: 'center',
      cursor: 'pointer',
      fontWeight: 500,
    },
  },
  {
    id: '202',
    data: { label: '二次函数' },
    position: { x: 350, y: 150 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: {
      background: '#ffffff',
      color: '#1e293b',
      border: '1px solid #94a3b8',
      borderRadius: '8px',
      width: 140,
      padding: '10px',
      fontSize: '14px',
      textAlign: 'center',
      cursor: 'pointer',
      fontWeight: 500,
    },
  },
  {
    id: '203',
    data: { label: '反比例函数' },
    position: { x: 350, y: 250 },
    sourcePosition: Position.Right,
    targetPosition: Position.Left,
    style: {
      background: '#ffffff',
      color: '#1e293b',
      border: '1px solid #94a3b8',
      borderRadius: '8px',
      width: 140,
      padding: '10px',
      fontSize: '14px',
      textAlign: 'center',
      cursor: 'pointer',
      fontWeight: 500,
    },
  },
];

//  (黑色曲线)
const initialEdges: Edge[] = [
  {
    id: 'e101-201',
    source: '101',
    target: '201',
    type: 'bezier',
    animated: false,
    style: { stroke: '#000000', strokeWidth: 1.5 },
  },
  {
    id: 'e101-202',
    source: '101',
    target: '202',
    type: 'bezier',
    style: { stroke: '#000000', strokeWidth: 1.5 },
  },
  {
    id: 'e101-203',
    source: '101',
    target: '203',
    type: 'bezier',
    style: { stroke: '#000000', strokeWidth: 1.5 },
  },
];

export const useKnowledgeGraph = () => {
  const [nodes, setNodes] = useState<Node[]>(initialNodes);
  const [edges, setEdges] = useState<Edge[]>(initialEdges);
  const [selectedNodeId, setSelectedNodeId] = useState<string | null>(null);
  const [isLoading] = useState(false);

  const onNodesChange: OnNodesChange = useCallback(
    (changes: NodeChange[]) =>
      setNodes((nds) => applyNodeChanges(changes, nds)),
    [],
  );

  const onEdgesChange: OnEdgesChange = useCallback(
    (changes: EdgeChange[]) =>
      setEdges((eds) => applyEdgeChanges(changes, eds)),
    [],
  );

  const onNodeClick = useCallback((_event: React.MouseEvent, node: Node) => {
    setSelectedNodeId(node.id);
  }, []);

  return {
    nodes,
    edges,
    onNodesChange,
    onEdgesChange,
    onNodeClick,
    selectedNodeId,
    isLoading,
  };
};
