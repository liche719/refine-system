INSERT INTO UserData(user_id, questions_num, review_rate, hard_questions, study_time)
VALUES ('demo-user', 1, 0, 1, 1) AS incoming
ON DUPLICATE KEY UPDATE questions_num = incoming.questions_num;

INSERT INTO knowledgePoint(user_id, knowledge_point_id, knowledge_point_name, knowledge_desc, subject, knowledge_level)
VALUES ('demo-user', 1, '二次函数', '二次函数图像、顶点与最值', '数学', 1) AS incoming
ON DUPLICATE KEY UPDATE knowledge_point_name = incoming.knowledge_point_name;

INSERT INTO MistakeQuestion(user_id, question_id, question_content, subject, knowledge_point_id, source)
VALUES ('demo-user', 'demo-question-1', '求函数 y=x^2-4x+3 的顶点坐标。', '数学', 1, 'demo') AS incoming
ON DUPLICATE KEY UPDATE question_content = incoming.question_content;
