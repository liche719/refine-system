// import { useState } from "react";
// import { getSimilarQuestion } from "@/services/apis/aiapi";

// export default function PracticePage() {
//   const [baseQuestion, setBaseQuestion] = useState("原题内容");
//   const [newQuestion, setNewQuestion] = useState("");
//   const [loading, setLoading] = useState(false);

//   const handleGenerate = async () => {
//     setLoading(true);
//     try {
//       const res = await getSimilarQuestion({ question: baseQuestion });
//       setNewQuestion(res.data);
//     } catch (error) {
//       console.error("生成题目失败", error);
//       setNewQuestion("生成失败，请重试");
//     } finally {
//       setLoading(false);
//     }
//   };

//   return (
//     <div className="p-6">
//       <h1 className="text-xl font-bold mb-4">AI相似题练习</h1>

//       <div className="bg-gray-50 p-4 rounded-lg shadow mb-4">
//         <label htmlFor="base-question" className="block font-medium mb-2">原题：</label>
//         <textarea
//           id="base-question"
//           value={baseQuestion}
//           onChange={(e) => setBaseQuestion(e.target.value)}
//           className="w-full p-2 border border-gray-300 rounded"
//           rows={4}
//         />
//       </div>

//       <button
//         onClick={handleGenerate}
//         className="mt-4 bg-blue-500 text-white px-4 py-2 rounded"
//         disabled={loading}
//       >
//         {loading ? "AI生成中..." : "生成相似题"}
//       </button>

//       {newQuestion && (
//         <div className="mt-4 bg-white p-4 rounded-lg shadow">
//           <h2 className="text-lg font-semibold mb-2">AI生成的新题</h2>
//           <p>{newQuestion}</p>
//         </div>
//       )}
//     </div>
//   );
// }
