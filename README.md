<div align="center">

# ⏳ 时厘

**岁月不居，时节如流。**

像记账一样，记录你的时间去向。

[![Kotlin](https://img.shields.io/badge/Kotlin-Jetpack%20Compose-7F52FF?logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Platform](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](#)
[![License](https://img.shields.io/badge/License-MIT-green)](LICENSE)
[![Offline](https://img.shields.io/badge/数据-100%25%20本地-blueviolet)](#-隐私)

</div>

---

## 📖 关于时厘

钱花了可以记账，时间花了呢？

「时厘」是一个纯粹的个人时间记账 App：点一下开始计时，做完点一下结束，
时厘帮你把每一段专注时间分门别类地存下来，再用日历和统计告诉你——
**你的时间，到底去了哪里。**

没有账号、没有社交、没有红点，只有你和你的时间。

## 📥 下载安装

不想自己编译？直接去 [**Releases**](https://github.com/Corin023/shili/releases) 页面下载最新版 APK，安装到 Android 手机即可使用（Android 8.0 及以上）。

## 📱 界面预览

<div align="center">
  <img src="docs/screenshots/timer.png" width="280" alt="计时页"/>
  &nbsp;&nbsp;&nbsp;
  <img src="docs/screenshots/calendar.png" width="280" alt="日历页"/>
</div>

<p align="center"><sub>计时页 · 暖调莫兰迪配色 ｜ 日历页 · 每天的时间一目了然</sub></p>

## ✨ 功能

| 功能 | 说明 |
|------|------|
| ⏱️ **一键计时** | 圆形计时器，点开始、点结束，自动保存一条时间记录 |
| 🗂️ **自由分类** | 事项不限数量，支持二级分类（如 运动 → 瑜伽 / 羽毛球 / 跑步），随时增删改 |
| 📅 **日历视图** | 月历上直接显示每天专注总时长，点开看当天明细，支持手动补录与滑动删除 |
| 📊 **统计分析** | 今日 / 近 7 天 / 本周 / 近 30 天 / 本月 / 本年 / 自定义区间，柱状图 + 分类下钻 |
| 📤 **CSV 导出** | 统计页一键导出所选区间的全部记录为 CSV，方便备份与二次分析 |
| 🎨 **莫兰迪设计** | 暖调低饱和配色，简洁、低干扰，支持深色模式 |
| 🔒 **完全离线** | 所有数据存在本机 Room 数据库，不联网、不上传 |

## 🚀 快速开始

### 方式一：Android Studio（推荐）

1. 克隆仓库并用 Android Studio 打开

   ```bash
   git clone https://github.com/Corin023/shili.git
   ```

2. 等待 Gradle 同步完成
3. 连接手机或启动模拟器，点击 ▶ 运行

### 方式二：命令行

```bash
# 安装到已连接的设备
./gradlew installDebug

# 或只打包 APK
./gradlew assembleDebug
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

> 环境要求：Android SDK（minSdk 26 / targetSdk 34），JDK 17+

## 🏗️ 技术栈

| 领域 | 选型 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 数据库 | Room（SQLite） |
| 导航 | Navigation Compose |
| 架构 | MVVM（ViewModel + Repository） |

## 📂 项目结构

```
app/src/main/java/com/example/timetracker/
├── MainActivity.kt          # 入口
├── TimeTrackerApp.kt        # Application
├── data/                    # 数据层
│   ├── AppDatabase.kt       # Room 数据库
│   ├── Category.kt          # 分类实体（支持二级）
│   ├── TimeRecord.kt        # 时间记录实体
│   ├── *Dao.kt              # 数据访问对象
│   └── TimeTrackerRepository.kt
└── ui/
    ├── theme/               # 莫兰迪主题
    ├── timer/               # 计时页 + 分类管理
    ├── calendar/            # 日历页
    └── stats/               # 统计页
```

## 🗺️ 已知限制与计划

当前为 MVP 版本：

- 计时需保持 App 在前台，切出过久可能中断
- 暂无云端同步，换机需手动迁移（可先用 CSV 导出备份）

后续计划：后台计时 · 云端同步 · 桌面小组件

## 🔒 隐私

时厘不申请网络权限，不收集任何数据。你的时间记录只属于你的手机。

## 🤝 贡献

欢迎 Issue 和 Pull Request。

## 📄 开源协议

[MIT License](LICENSE) — 自由使用，保留署名即可。

---

<div align="center">

如果时厘帮你找回了一些时间，欢迎点个 ⭐

</div>
