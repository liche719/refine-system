INSERT INTO RenderBook(id, book_name, book_content, enabled)
VALUES (1, 'Refine 使用指南', '上传错题后，系统会生成知识点解释和针对性练习。', 1)
ON DUPLICATE KEY UPDATE book_name=VALUES(book_name);

INSERT INTO UserData(user_id,questions_num,review_rate,hard_questions,study_time)
VALUES ('demo-user',1,0,1,1)
ON DUPLICATE KEY UPDATE questions_num=VALUES(questions_num);

INSERT INTO knowledgePoint(user_id,knowledge_point_id,knowledge_point_name,knowledge_desc,subject,knowledge_level)
VALUES ('demo-user',1,'二次函数','二次函数图像、顶点与最值','数学',1)
ON DUPLICATE KEY UPDATE knowledge_point_name=VALUES(knowledge_point_name);

INSERT INTO MistakeQuestion(user_id,question_id,question_content,subject,knowledge_point_id,source)
VALUES ('demo-user','demo-question-1','求函数 y=x^2-4x+3 的顶点坐标。','数学',1,'demo')
ON DUPLICATE KEY UPDATE question_content=VALUES(question_content);
