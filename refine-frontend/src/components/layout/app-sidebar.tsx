import * as React from 'react';
import {
  BookOpen,
  House,
  Upload,
  List,
  BrainCog,
  MessageCircleQuestion,
} from 'lucide-react';

import { NavMain } from '@/components/layout/nav-main';
import { NavUser } from '@/components/layout/nav-user';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
  SidebarMenuButton,
} from '@/components/ui/sidebar';
import { useAtomValue } from 'jotai';
import { userAtom } from '../../atoms/user';

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  const user = useAtomValue(userAtom);

  // This is sample data.
  const data = {
    user: {
      name: user.userName,
      email: user.userAccount,
      avatar: user.avatar || '',
    },
    navMain: [
      {
        title: '主页',
        url: '/home',
        icon: House,
      },
      {
        title: '上传错题',
        url: '/upload-question',
        icon: Upload,
      },
      {
        title: '我的错题',
        url: '/my-question',
        icon: BookOpen,
      },
      {
        title: '知识点库',
        url: '/knowledge-base',
        icon: List,
      },
      {
        title: 'AI 解题',
        url: '/ai-explain',
        icon: MessageCircleQuestion,
      },
    ],
  };

  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <SidebarMenuButton>
          <BrainCog className="!size-6" />
          <span className="text-lg font-bold">Refine 智能错题</span>
        </SidebarMenuButton>
      </SidebarHeader>
      <SidebarContent>
        <NavMain items={data.navMain} />
      </SidebarContent>
      <SidebarFooter>
        <NavUser user={data.user} />
      </SidebarFooter>
      <SidebarRail />
    </Sidebar>
  );
}
