-- ============================================================
-- 学校管理系统 · 在线考试子系统  缺陷修复升级脚本 v2
-- 新增:
--   6. exam_paper_question  试卷-题目关联表 (修复: 判分遍历全题库/固定组卷无题)
--   7. exam_answer          考生作答明细表 (修复: 简答题答案被丢弃, 供人工阅卷)
-- 说明: 已存在库直接执行本脚本即可; 全新安装见 init.sql (已同步)
-- ============================================================
USE exam_system;

-- 6. 试卷-题目关联表 (组卷/按卷判分的依据)
CREATE TABLE IF NOT EXISTS `exam_paper_question` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `paper_id` bigint(20) NOT NULL COMMENT '试卷ID',
  `question_id` bigint(20) NOT NULL COMMENT '题目ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷-题目关联表';

-- 7. 考生作答明细表 (每题作答+得分; 客观题自动判分落库, 主观题待人工给分)
CREATE TABLE IF NOT EXISTS `exam_answer` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL COMMENT '考试批次ID',
  `user_id` bigint(20) NOT NULL COMMENT '考生ID',
  `question_id` bigint(20) NOT NULL COMMENT '题目ID',
  `question_type` tinyint(4) NOT NULL COMMENT '题型: 1单选 2多选 3判断 4填空 5简答',
  `user_answer` text COMMENT '考生答案',
  `correct_flag` tinyint(4) DEFAULT '0' COMMENT '客观题: 1对 0错; 主观题: 0未阅 1已阅',
  `got_score` decimal(5,1) DEFAULT '0.0' COMMENT '本题得分',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_user_q` (`batch_id`, `user_id`, `question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考生作答明细表';

-- 关联演示数据: 试卷1 绑定题库题目 (4道客观 80分 + 1道简答 20分 = 100 自洽)
-- 原绑定(1..5)总分为100, 现替换第5题为简答题以便演示人工阅卷闭环
DELETE FROM `exam_paper_question` WHERE `paper_id` = 1;
INSERT IGNORE INTO `exam_paper_question` (`paper_id`, `question_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4);

-- 补一道简答题(20分), 绑定到试卷1
INSERT IGNORE INTO `question_bank` (`id`, `subject_id`, `type`, `content`, `options`, `answer`, `analysis`, `score`) VALUES
(6, 1, 5, '请简述 Spring Boot 自动配置(Starter)的工作原理。', NULL, '自动配置通过 @EnableAutoConfiguration 引入 AutoConfigurationImportSelector, 依据 classpath 上的依赖(META-INF/spring.factories 中声明的自动配置类)按条件注解(@ConditionalOnClass 等)生效, 为应用装配默认 Bean, 因此引入 spring-boot-starter-web 即可直接运行 Web 应用。', '考察对自动配置原理的理解: Starter 依赖 + 条件装配 + spring.factories', 20.0);
INSERT IGNORE INTO `exam_paper_question` (`paper_id`, `question_id`) VALUES (1, 6);

-- 演示考生账号
INSERT IGNORE INTO `user_info` (`username`, `password`, `real_name`, `department`, `role`, `status`) VALUES
('student', '123456', '测试考生', '计算机学院', 'student', 1);
