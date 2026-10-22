# 星河软件站客户端（Xinghe Store Client）

一款为**老安卓设备**打造的「星河软件站」第三方客户端——最低支持 **Android 1.5（API 3）**，
同时兼容到 Android 15。没有 WebView、没有 Support Library、没有 Gradle，
只有 `org.json` + 手写界面，APK 体积控制在 1 MB 以内。

- 服务端接口：星河软件站 `/iosipa/old/` 适配层（纯 JSON，支持 gzip，服务端分页/图标生成）
- 客户端架构：基于 [F-Droid Client 0.45](https://f-droid.org/) 的工程组织方式衍生
- 开源协议：[GPL-3.0](LICENSE)（继承自 F-Droid Client 0.45）

## 功能特性

| 模块 | 说明 |
| --- | --- |
| 应用浏览 | 全部 / 安卓 / iOS / PWA 四个标签页，滑动切换，下拉式分页加载 |
| 搜索 | 按名称 / 简介 / 分类实时搜索（输入 2 字符即触发） |
| 排行榜 | 按真实下载量取前 50 |
| 详情 | 大小 / 版本 / 平台 / 系统要求 / 架构 / 分类 / 评分 / 金币价 |
| 下载 | 直链文件内置下载器（进度对话框 + 完成通知 + 一键打开）；网盘链接一键复制并跳转浏览器 |
| 网盘 | 百度 / 夸克 / 迅雷 / 小飞机网盘链接单独成按钮 |
| 收藏夹 | 本地 SQLite 收藏，跨会话保留 |
| 下载记录 | 本地记录浏览 / 下载行为，可一键清空 |
| 评论 | 查看评论（星级渲染） + 评分提交（1-5 星） |
| 账号 | QQ 邮箱注册（验证码 60 秒倒计时）、登录、资料编辑、每日签到金币 |
| 投稿 | 完整投稿表单（平台 / 版本 / 大小 / 网盘链接 / 金币定价） |
| 消息 | 自己上传应用收到的评论通知 |
| 设置 | 检查更新（服务端 `/old/version`）、清除图标缓存、关于 |
| 主题 | 星河渐变紫主题，自定义标题栏与标签栏 |

## 兼容性要点（Android 1.5 / API 3）

编译基准是 **android.jar (API 3)**，因此以下高版本 API 全部用兼容写法处理：

| 高版本 API | 兼容方案 |
| --- | --- |
| `Build.VERSION.SDK_INT`（API 4+） | 解析 `Build.VERSION.SDK` 字符串（`Utils.sdkInt()`） |
| `TabSpec.setIndicator(View)`（API 4+） | 反射调用，失败退回文字标签 |
| `ClipboardManager`（API 11+） | 反射调用 `android.content.ClipboardManager`，失败退回 `android.text.ClipboardManager` |
| `insertWithOnConflict`（API 8+） | `delete + insert` |
| `LruCache`（API 12+） | `SoftReference` + `HashMap` |
| `DownloadManager`（API 9+） | 自实现 `HttpURLConnection` 下载器 |
| `getExternalCacheDir`（API 8+） | `Environment.getExternalStorageDirectory()` |
| v2 APK 签名（API 24+） | `jarsigner` v1 签名（API 3 设备唯一支持） |

语言级别锁定 Java 6（无菱形语法、无 try-with-resources、无字符串 switch）。

## 构建（GitHub Actions）

APK **不在本地编译**，由 GitHub Actions 自动完成：

1. Push 到 `main` 或打 `v*` 标签（也可在 Actions 页手动 `workflow_dispatch`）
2. 工作流安装 JDK 8 + 32 位运行库（aapt/dx/zipalign 是 2014 年的 32 位二进制）
3. 下载 Android 1.5 平台包与 Build-Tools 19.1.0（带 sha1 校验，命中缓存则跳过）
4. 执行 `build.sh`：aapt → javac → dx → aapt add → zipalign → jarsigner
5. 产物 `xinghe-store.apk` 上传为 Artifact，Release 页自动附带

本地手动构建（可选，需要 JDK 8 + curl + unzip）：

```bash
bash build.sh
# 产物：dist/xinghe-store.apk
```

签名密钥通过仓库 Secrets 注入（`XINGHE_KEYSTORE_B64` / `XINGHE_KEYSTORE_PASSWORD` /
`XINGHE_KEY_ALIAS` / `XINGHE_KEY_PASSWORD`），未配置时 CI 会用临时密钥签名，
仅用于验证构建流程可复现。

## 服务端配套

客户端依赖星河软件站服务器的 `/iosipa/old/` 适配层（FastAPI，仅新增路由，
不影响任何既有接口）：

```
GET  /iosipa/old/version          版本检查
GET  /iosipa/old/apps             列表（platform/q/sort/page/limit）
GET  /iosipa/old/app              详情
GET  /iosipa/old/ranking          排行榜
GET  /iosipa/old/icon?app_id=     图标（无原图时服务端 PIL 生成）
GET  /iosipa/old/comments         评论列表
POST /iosipa/old/comment          发表评论
GET  /iosipa/old/download         下载解析（附网盘链接）
POST /iosipa/old/send_code        发送验证码
POST /iosipa/old/register         注册
POST /iosipa/old/login            登录
GET  /iosipa/old/me               个人信息
POST /iosipa/old/update_profile   更新资料
POST /iosipa/old/checkin          每日签到
GET  /iosipa/old/messages         消息通知
POST /iosipa/old/submit_app       投稿
```

## 目录结构

```
XingheStore/
├── AndroidManifest.xml        # minSdk 3，12 个 Activity
├── build.sh                   # 手动构建脚本（CI 同款）
├── res/
│   ├── drawable*/             # 图标 + 选择器/渐变等 XML 图形
│   ├── layout/                # 全部界面布局
│   └── values/                # 字符串 / 颜色 / 样式
├── src/com/xinghe/store/
│   ├── api/                   # 接口封装 + 数据模型
│   ├── data/DB.java           # SQLite 收藏夹 / 下载记录
│   ├── net/Http.java          # GET / 文件下载（gzip 手动解压）
│   ├── ui/                    # 图片加载器（软引用+磁盘缓存）/ 列表适配器
│   └── *.java                 # 各界面 Activity
└── .github/workflows/         # GitHub Actions 构建
```

## 开源协议

GPL-3.0。本项目为 F-Droid Client 0.45（GPL-3.0）的衍生作品，
依据 GPL-3.0 第 5 条以相同协议开源；F-Droid 的版权归其原作者所有。
