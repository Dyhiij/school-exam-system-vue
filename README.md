# 🏫 学校管理系统（企业级在线考试系统）

> **课程实训项目 · Spring Boot 3 + Vue3 全栈**
> 题库管理 / 在线考试 / 防作弊监控 / 智能批阅 / 成绩可视化分析

一套面向**企业内部培训考核**与**学校教学测评**的在线考试系统，支持 **系统管理员 · 考试管理员 · 阅卷人 · 考生** 四类角色，覆盖「出题 → 组卷 → 在线考试 → 防作弊 → 判分阅卷 → 成绩分析」全流程。客观题交卷即自动判分，主观题支持盲改与流水线协同阅卷；随机策略组卷为每位考生生成难度一致但题目不同的试卷，从源头杜绝抄袭。

> 📌 **项目来源说明**：本项目基于网上开源模板二次开发（课程实训）。本仓库的核心交付不是模板本身，而是模板之上完成的 **8 项真实缺陷定位与修复**（含 1 项刷分安全漏洞）、**基于测试用例的完整回归闭环**、以及**测试报告 / 缺陷修复说明 / 实测截图**组成的可验证文档体系（详见下方「测试与质量」章节）。相关修复已全部回归通过。

| 后端 | Spring Boot 3 · Spring Security · MyBatis-Plus · MySQL 8 · Redis |
|---|---|
| 前端 | Vue3 · Vite · Element Plus · Pinia · Vue Router |
| 部署 | Docker Compose（另附便携式免安装交付包） |
| 演示账号 | `admin / 123456`（系统管理员）· `student / 123456`（考生，阅卷人角色可经管理端分配） |

---

## ✨ 功能亮点

- **📚 全题型题库**：单选 / 多选 / 判断 / 填空（多空顺序）/ 简答 / 材料题，支持 Excel、Word 模板批量导入
- **🧩 双模式组卷**：固定试卷人工挑题；随机试卷按知识点/题型/难度配比，每位考生一套差异化试卷
- **🛡️ 全链路防作弊**：切屏计数警告（超限自动交卷）、禁复制粘贴、禁右键、防多标签双开、IP 异常登录检测、多端登录互踢、交卷后不可重复进入（后端 403 拦截）
- **✍️ 自动 + 人工双轨阅卷**：客观题交卷瞬间自动赋分；主观题支持盲改模式（隐藏考生信息）与流水线模式（每人只改一题）
- **💾 答题快照容灾**：答题进度定时写入本地缓存，断网 / 浏览器崩溃后重进不丢一题
- **📊 多维成绩分析**：及格率走势、分数段分布、高频错题榜单、知识点掌握度分析，支持成绩单 Excel 导出

---

## 🖼️ 界面预览

| 登录 | 在线考试（防切屏监控） |
|---|---|
| ![login](https://cdn.jsdelivr.net/gh/Dyhiij/school-exam-system-vue@main/docs/screenshots/login.png) | ![exam](https://cdn.jsdelivr.net/gh/Dyhiij/school-exam-system-vue@main/docs/screenshots/exam-room.png) |

| 防作弊拦截（重复进入 403） | 我的成绩 |
|---|---|
| ![anti-cheat](https://cdn.jsdelivr.net/gh/Dyhiij/school-exam-system-vue@main/docs/screenshots/anti-cheat.png) | ![my-scores](https://cdn.jsdelivr.net/gh/Dyhiij/school-exam-system-vue@main/docs/screenshots/my-scores.png) |

| 人工阅卷（给分操作） | 管理端成绩查询 |
|---|---|
| ![grading](https://cdn.jsdelivr.net/gh/Dyhiij/school-exam-system-vue@main/docs/screenshots/grading.png) | ![score-query](https://cdn.jsdelivr.net/gh/Dyhiij/school-exam-system-vue@main/docs/screenshots/score-query.png) |

---

## 🧱 技术架构

```text
┌─────────────────────────────────────────────────────────┐
│  Vue3 + Element Plus 前端（Vite 开发服务器 / Nginx 静态） │
│  请求拦截器：登录态校验 · 接口降级（后端离线→本地演示）    │
└──────────────────────┬──────────────────────────────────┘
                       │ axios（Vite 代理 → 127.0.0.1:8080）
┌──────────────────────▼──────────────────────────────────┐
│        Spring Boot 3 后端（Spring Security + JWT）        │
│  Controller → Service → MyBatis-Plus Mapper → MySQL 8    │
│  Redis：登录会话 / 在线状态 / 答题快照                    │
└──────────────────────────────────────────────────────────┘
```

- **权限模型**：Spring Security + JWT 无状态认证，四角色 RBAC 菜单权限 + 数据权限隔离
- **判分引擎**：交卷事务内按 `exam_paper_question` 关联表遍历该卷题目 → 客观题自动判分写 `exam_score`，主观题答案落 `exam_answer` 待人工阅卷
- **数据表**：用户 / 角色 / 题库 / 试卷 / 试卷题目关联 / 考试批次 / 答卷 / 成绩 / 系统配置 / 操作日志（`database/init.sql`）

---

## 🧪 测试与质量（v3 修复版，2026-09-09）

本版为**缺陷修复后的交付版**，基于软件测试课程流程对原模板实测，定位并修复 8 项缺陷，全部回归通过（测试过程详见 [测试报告](docs/在线考试系统_测试报告.md) 与 [缺陷修复说明](docs/学校管理系统_缺陷修复说明_v3.md)，配套 13 张实测截图）：

| # | 原缺陷 | 修复方案 | 验证 |
|---|---|---|---|
| 1 | 交卷成绩不落库 | 真实写入 `exam_score` | ✅ 截图 04 |
| 2 | 判分遍历全题库 | 经关联表**按卷判分** | ✅ 截图 03 |
| 3 | 主观题答案丢弃 | 明细落 `exam_answer`，可人工阅卷 | ✅ 截图 08–10 |
| 4 | 登录态刷新丢失 | 登录写 localStorage，刷新不掉线 | ✅ 截图 01 |
| 5 | 成绩/阅卷页假数据 | 全部对接真实接口 | ✅ 截图 06–07 |
| 6 | 管理端组卷不可用 | 试卷「配置题目」真实保存 | ✅ 截图 12–13 |
| 7 | 一键启动 MySQL 失败 | 修正 my.ini 相对路径解析 | ✅ 回归通过 |
| 8 | 重复交卷可覆盖成绩（刷分风险） | 后端 403 拒绝 + 前端进入拦截 | ✅ TC-04 实测 |

---

## 📂 仓库结构

```text
school-exam-system/
├── backend/               # Spring Boot 后端源码
│   ├── src/main/java/     #   controller / service / entity / mapper
│   ├── src/main/resources/application.yml
│   └── pom.xml
├── frontend/              # Vue3 + Vite 前端源码
│   ├── src/               #   views / components / router / store / utils
│   ├── package.json
│   └── vite.config.ts     # 开发代理（/api → 127.0.0.1:8080）
├── database/              # init.sql（建表）· seed.sql（演示数据）· upgrade_v2.sql
├── deploy/                # docker-compose.yml 容器化部署
├── docs/                  # 测试报告 · 缺陷修复说明 · 实测截图
│   └── screenshots/       # README 预览图
└── monitor/               # 交付打包辅助脚本（package_final.py）
```

> ℹ️ 完整交付包另含 1.5GB 便携运行环境（内置 JDK / Maven / MySQL / Node.js，**解压即用**），本仓库按开源惯例已通过 `.gitignore` 排除 `runtime/`、`backend/target/`、`frontend/node_modules/` 等，仅收录源码与文档。如需便携包可联系作者获取。

---

## 🚀 快速开始（自备环境）

```bash
# 1. 初始化数据库（MySQL 8，按需改 application.yml 连接信息）
mysql -uroot -p < database/init.sql
mysql -uroot -p school_exam < database/seed.sql

# 2. 启动后端（JDK 17+ / Maven，默认 8080 端口）
cd backend && mvn spring-boot:run

# 3. 启动前端（Node.js 18+）
cd frontend && npm install && npm run dev
# 浏览器访问 http://localhost:5173 （admin/123456）
```

也可一键容器化部署：`docker compose -f deploy/docker-compose.yml up -d`

---

## 📋 完整交付说明（便携绿色版运行指南）

> ## 📌 v3 修复版说明（2026-09-09）
>
> 本包为**缺陷修复后的交付版**，相对原模板修复了 8 项缺陷（详见课程交付文档
> 《学校管理系统_缺陷修复说明_v3.md》）：
>
> 1. 交卷成绩不落库 → 真实写入 `exam_score`
> 2. 判分遍历全题库 → 经 `exam_paper_question` 关联表按卷判分
> 3. 主观题答案丢弃 → 明细落 `exam_answer`，教师可人工阅卷
> 4. 登录态刷新丢失 → 登录写 localStorage，刷新不掉线
> 5. 成绩/阅卷页面假数据 → 全部对接真实接口
> 6. 管理端组卷不可用 → 试卷「配置题目」真实保存
> 7. 一键启动 MySQL 失败 → 修正 my.ini 相对路径解析
> 8. 重复交卷可覆盖成绩（刷分风险）→ 后端 403 拒绝 + 前端进入拦截 + 交卷后切屏不再警告（v3 新增，软件测试 TC-04 实测发现）
>
> **启动方式**：双击根目录 `启动系统.bat`，浏览器访问 http://localhost:5173
> （账号 admin/123456、student/123456）。

便携版把免安装运行环境（Java / Maven / MySQL / Node.js）放在 `runtime/` 目录，**无需配置任何环境变量**，复制到任意 Windows 电脑即可运行：

### 1️⃣ 启动 Redis（终端 1）
```powershell
cd runtime\redis
.\redis-server.exe redis.windows.conf
```

### 2️⃣ 启动 MySQL（终端 2）
```powershell
cd runtime\mysql
.\bin\mysqld.exe --defaults-file="my.ini" --console
```
> 首次使用若提示需要初始化，先执行 `.\bin\mysqld.exe --initialize-insecure --console`

### 3️⃣ 启动 Spring Boot 后端（终端 3）
```powershell
cd backend
..\runtime\maven\bin\mvn.cmd clean package -DskipTests   # 首次编译
..\runtime\java\bin\java.exe -jar target\exam-system-1.0.0.jar
```

### 4️⃣ 启动 Vue3 前端（终端 4）
```powershell
cd frontend
$env:Path = "..\runtime\nodejs;" + $env:Path
npm install
npm run dev
```
浏览器访问 http://localhost:5173

> 💡 建议用 VS Code 打开项目根目录，开 4 个终端分别执行以上步骤。
> 若遇端口占用，`Ctrl+C` 停掉对应服务再试。

---

## 📄 文档索引

| 文档 | 说明 |
|---|---|
| [测试报告](docs/在线考试系统_测试报告.md) | 软件测试课程交付：13 条用例 + 实测截图 + SQL 证据 |
| [缺陷修复说明 v3](docs/学校管理系统_缺陷修复说明_v3.md) | 8 项缺陷的根因分析与修复验证 |
| [实测截图](docs/测试截图/) | 全流程 13 张功能验证截图 |

---

> 🎉 **祝你编码愉快！** 本仓库代码基于 MIT 协议开源，欢迎 Star / Fork / Issue。
