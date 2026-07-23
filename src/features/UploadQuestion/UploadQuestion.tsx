import { useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  BotMessageSquare,
  CloudUpload,
  FileCheck2,
  FileText,
  Lightbulb,
  ScanText,
  X,
} from 'lucide-react';
import { toast } from 'sonner';

import { Button } from '@/components/ui/button';
import { extractFirst } from '@/services/ocr/uploadQuestion';
import { errorMessage } from '@/utils/api';

const MAX_SIZE = 10 * 1024 * 1024;
const ACCEPTED = ['txt', 'docx', 'pdf', 'png', 'jpg', 'jpeg', 'webp'];

export default function UploadQuestionPage() {
  const navigate = useNavigate();
  const inputRef = useRef<HTMLInputElement>(null);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [uploading, setUploading] = useState(false);

  const select = (file?: File) => {
    if (!file) return;
    const extension = file.name.split('.').pop()?.toLowerCase() || '';
    if (!ACCEPTED.includes(extension))
      return toast.error('仅支持图片、PDF、DOCX 和 TXT 文件');
    if (file.size > MAX_SIZE) return toast.error('文件不能超过 10 MB');
    if (!file.size) return toast.error('文件内容为空');
    setSelectedFile(file);
  };

  const upload = async () => {
    if (!selectedFile) return toast.info('请先选择文件');
    setUploading(true);
    try {
      const extension = selectedFile.name.split('.').pop()?.toLowerCase() || '';
      const response = await extractFirst(selectedFile, extension);
      if (response.code !== 200) throw new Error(response.info);
      toast.success(
        response.data.knowledgePoint
          ? `题目已加入错题库，并关联「${response.data.knowledgePoint}」`
          : '题目已加入错题库；暂未识别到可靠知识点',
      );
      navigate(
        `/upload-question/question-detail?questionId=${encodeURIComponent(response.data.questionId)}`,
        {
          state: { result: response },
        },
      );
    } catch (error) {
      toast.error(errorMessage(error, '上传解析失败'));
    } finally {
      setUploading(false);
    }
  };

  return (
    <main className="app-page upload-page">
      <header className="page-heading">
        <div>
          <p className="page-kicker">CAPTURE A MISTAKE</p>
          <h1>上传错题</h1>
          <p>支持图片、PDF、DOCX 与文本，单个文件最大 10 MB。</p>
        </div>
      </header>

      <div className="upload-layout">
        <section className="upload-workspace">
          <button
            type="button"
            className={`drop-zone ${isDragging ? 'dragging' : ''} ${selectedFile ? 'selected' : ''}`}
            onClick={() => inputRef.current?.click()}
            onDragOver={(event) => {
              event.preventDefault();
              setIsDragging(true);
            }}
            onDragLeave={() => setIsDragging(false)}
            onDrop={(event) => {
              event.preventDefault();
              setIsDragging(false);
              select(event.dataTransfer.files[0]);
            }}
          >
            <input
              ref={inputRef}
              type="file"
              hidden
              accept=".txt,.docx,.pdf,.png,.jpg,.jpeg,.webp"
              onChange={(event) => select(event.target.files?.[0])}
            />
            {selectedFile ? (
              <>
                <FileCheck2 className="size-12" />
                <strong>{selectedFile.name}</strong>
                <span>{(selectedFile.size / 1024 / 1024).toFixed(2)} MB</span>
              </>
            ) : (
              <>
                <CloudUpload className="size-12" />
                <strong>拖放文件，或点击选择</strong>
                <span>PNG · JPG · WEBP · PDF · DOCX · TXT</span>
              </>
            )}
          </button>
          <div className="upload-actions">
            {selectedFile && (
              <Button
                type="button"
                variant="ghost"
                onClick={() => setSelectedFile(null)}
              >
                <X className="size-4" /> 移除
              </Button>
            )}
            <Button onClick={upload} disabled={!selectedFile || uploading}>
              {uploading ? '正在识别...' : '开始识别'}
              <ScanText className="size-4" />
            </Button>
          </div>
        </section>

        <aside className="upload-process">
          <p className="page-kicker">WHAT HAPPENS NEXT</p>
          <h2>从文件到可复习错题</h2>
          {[
            {
              icon: FileText,
              title: '提取题干',
              text: '识别文件中的第一道题目。',
            },
            {
              icon: Lightbulb,
              title: '沉淀知识点',
              text: '保存错题并关联学习上下文。',
            },
            {
              icon: BotMessageSquare,
              title: '生成解析',
              text: '通过流式 AI 获得解题思路。',
            },
          ].map(({ icon: Icon, title, text }, index) => (
            <article key={title}>
              <span>{index + 1}</span>
              <Icon className="size-5" />
              <div>
                <strong>{title}</strong>
                <p>{text}</p>
              </div>
            </article>
          ))}
        </aside>
      </div>
    </main>
  );
}
