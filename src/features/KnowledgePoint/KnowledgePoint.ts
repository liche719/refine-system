import { useState, useEffect } from 'react';
import { toast } from 'sonner';
import {
  fetchRootPoints,
  fetchDefinition,
  fetchRelatedQuestionsOrNote,
  fetchRelatedPoints,
  fetchTooltip,
  markAsMastered,
  saveNote,
  renamePoint,
  addSonPoint,
  KnowledgePointNode,
  RelatedData,
  KnowledgeTooltip,
} from '@/services/apis/KnowledgePointApi';

export const useKnowledgePage = () => {
  const [rootPoints, setRootPoints] = useState<KnowledgePointNode[]>([]);
  const [activeId, setActiveId] = useState<number | null>(null);
  const [activeTitle, setActiveTitle] = useState('');
  const [refreshTreeTrigger, setRefreshTreeTrigger] = useState(0);

  const [description, setDescription] = useState('');
  const [relatedData, setRelatedData] = useState<RelatedData | null>(null);
  const [relatedPoints, setRelatedPoints] = useState<KnowledgePointNode[]>([]);
  const [isMastered, setIsMastered] = useState(false);
  const [stats, setStats] = useState<KnowledgeTooltip | null>(null);

  const [noteInput, setNoteInput] = useState('');
  const [isRenameOpen, setIsRenameOpen] = useState(false);
  const [isAddChildOpen, setIsAddChildOpen] = useState(false);

  const [newName, setNewName] = useState('');
  const [newChildName, setNewChildName] = useState('');
  const [newChildDesc, setNewChildDesc] = useState('');
  const [operatingId, setOperatingId] = useState<number | null>(null);

  // 1. 初始化：获取根节点
  useEffect(() => {
    const fetchRoot = async () => {
      try {
        const res = await fetchRootPoints({ subject: '数学' });
        if (res.success) setRootPoints(res.data);
      } catch {
        toast.error('知识点加载失败，请稍后重试');
      }
    };
    fetchRoot();
  }, []);

  // 2. 选中知识点：加载所有详情数据
  useEffect(() => {
    if (!activeId) return;

    // 重置 UI 状态
    setDescription('加载中...');
    setRelatedData(null);
    setNoteInput('');
    setRelatedPoints([]);
    setStats(null);

    // A. 获取定义
    fetchDefinition(activeId)
      .then((res) => {
        if (res.success) {
          setDescription(res.data.content || '暂无详细描述');
          setIsMastered(!!res.data.isMastered);
        } else {
          setDescription('暂无详细描述 (未找到定义)');
        }
      })
      .catch(() => setDescription('暂无详细描述'));

    // B. 获取错题和笔记
    fetchRelatedQuestionsOrNote(activeId).then((res) => {
      if (res.success) {
        setRelatedData(res.data);
        setNoteInput(res.data.note || '');
      }
    });

    // C. 获取相关知识点
    fetchRelatedPoints(activeId).then((res) => {
      if (res.success) setRelatedPoints(res.data);
    });

    // D. 获取统计数据 (用于仪表盘)
    fetchTooltip(activeId).then((res) => {
      if (res.success) setStats(res.data);
    });
  }, [activeId]);

  // --- 交互处理函数 ---

  const handleMarkMastered = async () => {
    if (!activeId) return;
    const res = await markAsMastered(activeId);
    if (res.success) {
      setIsMastered(true);
    }
  };

  const handleSaveNote = async () => {
    if (!activeId) return;
    await saveNote(activeId, noteInput);
  };

  const handleRename = async () => {
    if (!activeId || !newName.trim()) return;
    const res = await renamePoint(activeId, newName);
    if (res.success) {
      setActiveTitle(newName);
      setRefreshTreeTrigger((p) => p + 1); // 触发树刷新
      // 刷新根节点以防万一
      fetchRootPoints({ subject: '数学' }).then((r) => {
        if (r.success) setRootPoints(r.data);
      });
      setIsRenameOpen(false);
    }
  };

  const handleAddChild = async () => {
    if (!operatingId || !newChildName.trim()) return;
    const res = await addSonPoint(operatingId, {
      pointName: newChildName,
      pointDesc: newChildDesc,
    });
    if (res.success) {
      setRefreshTreeTrigger((p) => p + 1);
      setIsAddChildOpen(false);
      setNewChildName('');
      setNewChildDesc('');
    }
  };

  // --- 辅助函数 ---
  const openAddChildDialog = (parentId: number) => {
    setOperatingId(parentId);
    setIsAddChildOpen(true);
  };

  const openRenameDialog = () => {
    setNewName(activeTitle);
    setIsRenameOpen(true);
  };

  return {
    // Data
    rootPoints,
    activeId,
    setActiveId,
    activeTitle,
    setActiveTitle,
    description,
    relatedData,
    relatedPoints,
    isMastered,
    stats,
    refreshTreeTrigger,

    // Form & UI State
    noteInput,
    setNoteInput,
    isRenameOpen,
    setIsRenameOpen,
    isAddChildOpen,
    setIsAddChildOpen,
    newName,
    setNewName,
    newChildName,
    setNewChildName,
    newChildDesc,
    setNewChildDesc,

    // Actions
    handleMarkMastered,
    handleSaveNote,
    handleRename,
    handleAddChild,
    openAddChildDialog,
    openRenameDialog,
  };
};
