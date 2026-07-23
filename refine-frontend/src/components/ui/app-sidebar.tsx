import * as React from 'react';
import { BookOpen, House, Upload, List, BrainCog } from 'lucide-react';

import { NavMain } from '@/components/ui/nav-main';
import { NavUser } from '@/components/ui/nav-user';
import {
  Sidebar,
  SidebarContent,
  SidebarFooter,
  SidebarHeader,
  SidebarRail,
  SidebarMenuButton,
} from '@/components/ui/sidebar';

// This is sample data.
const data = {
  user: {
    name: 'shadcn',
    email: 'm@example.com',
    avatar: '/avatars/shadcn.jpg',
  },
  navMain: [
    {
      title: '主页',
      url: '/home',
      icon: House,
    },
    {
      title: '上传错题',
      url: '/home/upload-question',
      icon: Upload,
    },
    {
      title: '我的错题',
      url: '/home/my-question',
      icon: BookOpen,
    },
    {
      title: '知识点库',
      url: '/home/knowledge-base',
      icon: List,
    },
  ],
};

export function AppSidebar({ ...props }: React.ComponentProps<typeof Sidebar>) {
  return (
    <Sidebar collapsible="icon" {...props}>
      <SidebarHeader>
        <SidebarMenuButton>
          <BrainCog className="!size-6" />
          <span className="text-2xl font-bold">智能错题系统</span>
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
