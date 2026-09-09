USE exam_system;

CREATE TABLE IF NOT EXISTS `exam_paper` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL,
  `type` tinyint DEFAULT '1',
  `total_score` decimal(5,1) DEFAULT NULL,
  `pass_score` decimal(5,1) DEFAULT NULL,
  `duration` int DEFAULT NULL,
  `status` tinyint DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `question_bank` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `subject_id` bigint DEFAULT NULL,
  `type` int DEFAULT NULL,
  `content` text,
  `options` text,
  `answer` text,
  `analysis` text,
  `difficulty` int DEFAULT NULL,
  `score` decimal(5,1) DEFAULT NULL,
  `create_by` varchar(64) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO exam_paper (title, type, total_score, pass_score, duration, status) VALUES ('Java高级开发工程师考核 (真实数据)', 1, 100, 60, 120, 1);
INSERT INTO question_bank (subject_id, type, content, options, answer, score, difficulty) VALUES (1, 1, 'Java中哪个修饰符表示不能被继承？', '{"A":"static", "B":"final", "C":"abstract", "D":"volatile"}', 'B', 50, 1);
INSERT INTO question_bank (subject_id, type, content, options, answer, score, difficulty) VALUES (1, 1, 'Spring Boot的默认端口是？', '{"A":"8080", "B":"3306", "C":"6379", "D":"5173"}', 'A', 50, 1);
