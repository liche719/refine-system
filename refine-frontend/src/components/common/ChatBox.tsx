import { useState, useEffect, useRef } from 'react';
import { Plus, Upload } from 'lucide-react';
import gsap from 'gsap';
import { useGSAP } from '@gsap/react';

import { Button } from '@/components/ui/button';
import { Textarea } from '@/components/ui/textarea';

type Message = {
  role: 'user' | 'assistant';
  content: string;
};

export default function ChatBox() {
  const [message, setMessage] = useState<Message[]>([
    {
      role: 'assistant',
      content: '欢迎使用智能错题提问系统，请您根据什么问题提问',
    },
  ]);
  const [input, setInput] = useState('');
  const [expended, setExpended] = useState(false);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const scrollContainerRef = useRef<HTMLDivElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const container = scrollContainerRef.current;
    if (container) {
      container.scrollTo({
        top: container.scrollHeight,
        behavior: 'smooth',
      });
    }
  }, [message]);

  useGSAP(
    () => {
      if (expended) {
        // 展开动画
        const tl = gsap.timeline();
        tl.to('.expended-btn', {
          height: '80px',
          opacity: 0,
          duration: 0.3,
          ease: 'power2.inOut',
        }).to(
          '.textarea-area',
          {
            opacity: 1,
            y: 0,
            duration: 0.4,
            ease: 'power3.inOut',
          },
          '-=0.1',
        );
      }
    },
    { dependencies: [expended], scope: containerRef },
  );

  const handleSubmit = (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (input.trim()) {
      setMessage([...message, { role: 'user', content: input }]);
      setInput('');
    }
  };

  return (
    <div className="flex flex-col h-full space-y-4">
      <div className="rounded-lg bg-muted p-4 text-sm text-muted-foreground flex-1 flex flex-col min-h-0">
        <div
          ref={scrollContainerRef}
          className="flex-1 overflow-y-auto min-h-0"
        >
          {message.map((msg, idx) => {
            return (
              <div
                key={idx}
                className={`flex ${msg.role === 'user' ? 'justify-end' : 'justify-start'} mb-2`}
              >
                <div
                  className={`p-2 rounded-md max-w-3/4 ${msg.role === 'user' ? 'bg-primary text-white' : 'bg-secondary text-secondary-foreground'}`}
                >
                  {msg.content}
                </div>
              </div>
            );
          })}
        </div>
        <div ref={messageEndRef}></div>
      </div>
      <div ref={containerRef} className="relative">
        <Button
          className="expended-btn w-full shadow-md cursor-pointer z-10"
          onClick={() => setExpended(true)}
        >
          <Plus className="size-4 mr-2" />
          请用自然语言提问
        </Button>
        <form
          className="textarea-area absolute w-full opacity-0 top-0 left-0 translate-y-[80px] min-h-[120px] z-0"
          onSubmit={handleSubmit}
        >
          <div className="relative">
            <Textarea
              className="
              w-full resize-none
              border border-muted-foreground/30
              focus-visible:ring-0 focus-visible:ring-offset-0 focus-visible:border-muted-foreground/30
              focus:outline-none
              transition-colors
              "
              value={input}
              onChange={(e) => setInput(e.target.value)}
              placeholder="请输入问题"
            />
            <Button
              size="icon"
              className="absolute right-2 bottom-2 rounded-full"
              type="submit"
            >
              <Upload className="size-4" />
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
