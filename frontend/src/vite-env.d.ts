declare module '*.css' {
  const content: Record<string, string>;
  export default content;
}

interface ImportMetaEnv {
  readonly VITE_BASE_URL: string;
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}

/* vite/client 会让 TypeScript 认识 import.meta

ImportMetaEnv 是你的环境变量类型

ImportMeta 扩展 env 属性，让 TS 不再报错 */
