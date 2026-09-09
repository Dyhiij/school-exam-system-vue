-- 数据库初始化脚本
-- 对应方案中的“企业级在线考试系统”

CREATE DATABASE IF NOT EXISTS `exam_system` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE `exam_system`;

-- 1. 用户表 (user_info)
CREATE TABLE IF NOT EXISTS `user_info` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '工号/准考证号',
  `password` varchar(100) NOT NULL COMMENT '加密密码',
  `real_name` varchar(50) DEFAULT NULL COMMENT '真实姓名',
  `department` varchar(50) DEFAULT NULL COMMENT '部门',
  `role` varchar(20) DEFAULT 'student' COMMENT '角色: student, teacher, admin',
  `status` tinyint(4) DEFAULT '1' COMMENT '状态: 1正常 0禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户信息表';

-- 初始管理员账号: admin / 123456 (需加密存储，此处仅示例)
INSERT IGNORE INTO `user_info` (`username`, `password`, `real_name`, `role`) VALUES ('admin', '123456', '系统管理员', 'admin');

-- 2. 题库表 (question_bank)
CREATE TABLE IF NOT EXISTS `question_bank` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `subject_id` bigint(20) NOT NULL COMMENT '科目ID',
  `type` tinyint(4) NOT NULL COMMENT '题型: 1单选 2多选 3判断 4填空 5简答',
  `content` text NOT NULL COMMENT '题干',
  `options` text COMMENT '选项(JSON格式)',
  `answer` text NOT NULL COMMENT '参考答案',
  `analysis` text COMMENT '解析',
  `difficulty` tinyint(4) DEFAULT '1' COMMENT '难度: 1简单 2中等 3困难',
  `score` decimal(5,1) DEFAULT '0.0' COMMENT '默认分值',
  `create_by` varchar(50) DEFAULT NULL,
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_subject` (`subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- 3. 试卷表 (exam_paper)
CREATE TABLE IF NOT EXISTS `exam_paper` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `title` varchar(100) NOT NULL COMMENT '试卷名称',
  `type` tinyint(4) DEFAULT '1' COMMENT '组卷方式: 1固定 2随机',
  `total_score` decimal(5,1) DEFAULT '100.0' COMMENT '总分',
  `pass_score` decimal(5,1) DEFAULT '60.0' COMMENT '及格分',
  `duration` int(11) DEFAULT '60' COMMENT '考试时长(分钟)',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0草稿 1发布',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷表';

-- 4. 考试批次表 (exam_batch)
CREATE TABLE IF NOT EXISTS `exam_batch` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `paper_id` bigint(20) NOT NULL COMMENT '关联试卷ID',
  `title` varchar(100) NOT NULL COMMENT '考试名称',
  `start_time` datetime NOT NULL COMMENT '开始时间',
  `end_time` datetime NOT NULL COMMENT '结束时间',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0未开始 1进行中 2已结束',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_paper` (`paper_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试批次表';

-- 5. 考试成绩表 (exam_score)
CREATE TABLE IF NOT EXISTS `exam_score` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `batch_id` bigint(20) NOT NULL COMMENT '考试批次ID',
  `user_id` bigint(20) NOT NULL COMMENT '考生ID',
  `total_score` decimal(5,1) DEFAULT '0.0' COMMENT '总得分',
  `status` tinyint(4) DEFAULT '0' COMMENT '状态: 0未交卷 1已交卷 2已阅卷 3已发布',
  `submit_time` datetime DEFAULT NULL COMMENT '交卷时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `del_flag` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_user` (`batch_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试成绩表';

-- 插入真实测试数据
INSERT IGNORE INTO `exam_paper` (`id`, `title`, `total_score`, `pass_score`, `duration`, `status`) VALUES 
(1, '2026年企业级Java高级开发工程师认证考试', 100.0, 60.0, 120, 1);

INSERT IGNORE INTO `question_bank` (`id`, `subject_id`, `type`, `content`, `options`, `answer`, `analysis`, `score`) VALUES
(1, 1, 1, 'Spring Boot的默认端口是多少？', '{"A":"8080","B":"80","C":"3306","D":"6379"}', 'A', 'Spring Boot内嵌Tomcat，默认端口为8080', 20.0),
(2, 1, 1, '在Vue3中，哪个函数用于定义响应式数据？', '{"A":"reactive","B":"ref","C":"both","D":"none"}', 'C', 'ref和reactive都可以用于定义响应式数据', 20.0),
(3, 1, 3, 'MyBatis-Plus中，BaseMapper接口提供了常用的CRUD方法。', '{"A":"正确","B":"错误"}', 'A', 'BaseMapper确实提供了基础的CRUD操作', 20.0),
(4, 1, 1, 'Redis的默认端口是多少？', '{"A":"6379","B":"3306","C":"8080","D":"1521"}', 'A', 'Redis的默认端口是6379', 20.0),
(5, 1, 1, '下列哪个不是Java的基本数据类型？', '{"A":"int","B":"String","C":"boolean","D":"char"}', 'B', 'String是引用数据类型，不是基本数据类型', 20.0);

INSERT IGNORE INTO `exam_batch` (`id`, `paper_id`, `title`, `start_time`, `end_time`, `status`) VALUES
(1, 1, '2026春季第一批次', '2026-01-01 00:00:00', '2026-12-31 23:59:59', 1);

-- ============================================================
-- 缺陷修复 v2 新增表
-- ============================================================

-- 6. 试卷-题目关联表 (修复: 判分遍历全题库/固定组卷无题)
CREATE TABLE IF NOT EXISTS `exam_paper_question` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `paper_id` bigint(20) NOT NULL COMMENT '试卷ID',
  `question_id` bigint(20) NOT NULL COMMENT '题目ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_paper_question` (`paper_id`, `question_id`),
  KEY `idx_question` (`question_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷-题目关联表';

-- 7. 考生作答明细表 (修复: 简答题答案被丢弃, 供人工阅卷)
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
DELETE FROM `exam_paper_question` WHERE `paper_id` = 1;
INSERT IGNORE INTO `exam_paper_question` (`paper_id`, `question_id`) VALUES
(1, 1), (1, 2), (1, 3), (1, 4);

-- 补一道简答题(20分), 绑定到试卷1 (演示人工阅卷闭环)
INSERT IGNORE INTO `question_bank` (`id`, `subject_id`, `type`, `content`, `options`, `answer`, `analysis`, `score`) VALUES
(6, 1, 5, '请简述 Spring Boot 自动配置(Starter)的工作原理。', NULL, '自动配置通过 @EnableAutoConfiguration 引入 AutoConfigurationImportSelector, 依据 classpath 上的依赖(META-INF/spring.factories 中声明的自动配置类)按条件注解(@ConditionalOnClass 等)生效, 为应用装配默认 Bean, 因此引入 spring-boot-starter-web 即可直接运行 Web 应用。', '考察对自动配置原理的理解: Starter 依赖 + 条件装配 + spring.factories', 20.0);
INSERT IGNORE INTO `exam_paper_question` (`paper_id`, `question_id`) VALUES (1, 6);

-- 演示考生账号
INSERT IGNORE INTO `user_info` (`username`, `password`, `real_name`, `department`, `role`, `status`) VALUES
('student', '123456', '测试考生', '计算机学院', 'student', 1);

