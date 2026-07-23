import { useState, useEffect } from 'react';
import { KnowledgePointNode } from '@/services/apis/KnowledgePointApi';

interface MockDataStats {
  count: number;
  reason: string;
  review: number;
}

interface MockDataDetail {
  title: string;
  desc: string;
  note: string;
  questions: number[];
  stats: MockDataStats;
}

const MOCK_DB: Record<number, MockDataDetail> = {
  101: {
    title: '函数概念',
    desc: '函数（function）的定义：给定一个数集A...',
    note: '重点在于理解定义域和值域的关系。',
    questions: [10001, 10002],
    stats: { count: 4, reason: '概念模糊', review: 2 },
  },
  201: {
    title: '一次函数',
    desc: '一次函数是函数中的一种，一般形如 y=kx+b...',
    note: 'k>0, y随x增大而增大...',
    questions: [20001, 20002],
    stats: { count: 2, reason: '计算错误', review: 5 },
  },
  202: {
    title: '二次函数',
    desc: '二次函数最高次必须为二次...',
    note: '记忆顶点坐标公式...',
    questions: [30001],
    stats: { count: 3, reason: '公式记错', review: 1 },
  },
  203: {
    title: '反比例函数',
    desc: '反比例函数形如 y=k/x...',
    note: '注意自变量 x 不能为 0。',
    questions: [40001],
    stats: { count: 1, reason: '审题不清', review: 0 },
  },
};

export const useKnowledgePage = () => {
  const [activeId, setActiveId] = useState<number | null>(null);
  const [activeTitle, setActiveTitle] = useState<string>('');

  const [description, setDescription] = useState<string>('');
  const [noteInput, setNoteInput] = useState<string>('');

  const [viewStats, setViewStats] = useState<MockDataStats | null>(null);
  const [relatedPoints, setRelatedPoints] = useState<KnowledgePointNode[]>([]);

  // 当前关联的错题 IDs
  const [mistakeIds, setMistakeIds] = useState<number[]>([]);

  useEffect(() => {
    if (!activeId) return;

    const data = MOCK_DB[activeId];

    if (data) {
      setActiveTitle(data.title);
      setDescription(data.desc);
      setNoteInput(data.note);
      setViewStats(data.stats);
      setMistakeIds(data.questions); // 设置错题 ID

      const mockRelated: KnowledgePointNode[] = [
        { id: 301, keyPoints: '映射', hasChildren: true },
        { id: 302, keyPoints: '定义域', hasChildren: true },
        { id: 303, keyPoints: '值域', hasChildren: true },
      ];
      setRelatedPoints(mockRelated);
    } else {
      setActiveTitle('未知知识点');
      setDescription('暂无描述');
      setNoteInput('');
      setViewStats({ count: 0, reason: '无', review: 0 });
      setMistakeIds([]);
      setRelatedPoints([]);
    }
  }, [activeId]);

  const handleSaveNote = () => {
    console.log(`保存 [${activeTitle}] 的笔记:`, noteInput);
  };

  return {
    activeId,
    setActiveId,
    activeTitle,
    description,
    viewStats,
    relatedPoints,
    noteInput,
    setNoteInput,
    handleSaveNote,
    mistakeIds,
  };
};
