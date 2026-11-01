# 极简待办 MinimalTodo

一个用 Java + Android 基础组件实现的极简待办清单，包含提醒功能。

## 功能

- 任务增、删、改、查
- 完成状态勾选
- 分类筛选（工作 / 学习 / 生活 / 其他）
- 标题与备注搜索
- 截止时间 + AlarmManager 通知提醒
- 逾期任务红色标注
- 滑动删除 + 撤销
- 深色模式（跟随系统 / 浅色 / 深色）
- 显示 / 隐藏已完成任务
- 精确提醒开关（Android 12+）
- 导出 JSON

## 页面

| 页面 | 说明 |
| --- | --- |
| `MainActivity` + `TaskListFragment` | 列表页：搜索、分类、任务列表、FAB 新建 |
| `EditTaskActivity` | 编辑页：标题、备注、分类、截止时间、提醒开关 |
| `SettingsActivity` | 设置页：主题、显示已完成、通知 / 精确提醒、导出 JSON、清除已完成 |

## 代码结构

```
app/src/main/java/com/example/minimaltodo/
├── data/
│   ├── Task.java              Room 实体
│   ├── TaskDao.java           Room DAO
│   ├── TaskDatabase.java      Room 数据库
│   └── TaskRepository.java    数据仓库（后台线程访问数据库）
├── reminder/
│   ├── ReminderScheduler.java AlarmManager 设置 / 取消提醒
│   ├── ReminderReceiver.java  接收闹钟并发出通知
│   ├── ReminderNotifier.java  构建提醒通知
│   └── BootReceiver.java      开机 / 更新后恢复提醒
├── util/
│   ├── Prefs.java             SharedPreferences（主题等）
│   ├── DateFormats.java       日期格式化
│   └── JsonExporter.java      导出 JSON
├── MainActivity.java          Activity + Fragment 容器
├── TaskListFragment.java      列表页 Fragment
├── TaskAdapter.java           RecyclerView ListAdapter
├── TaskViewModel.java         ViewModel + LiveData（搜索 / 分类过滤）
├── EditTaskActivity.java      编辑页
├── SettingsActivity.java      设置页
└── TodoApp.java               Application（主题、通知渠道）
```

## 课程知识点对应

| 课程内容 | 使用位置 |
| --- | --- |
| Activity / Fragment | `MainActivity` 承载 `TaskListFragment`，编辑页 / 设置页为独立 Activity |
| RecyclerView | `TaskAdapter`（`ListAdapter` + `DiffUtil`） |
| Intent | 打开编辑页、设置页、通知点击跳转 |
| Room | `Task` / `TaskDao` / `TaskDatabase` |
| ViewModel + LiveData | `TaskViewModel`，页面随数据自动刷新 |
| SharedPreferences | `Prefs` 保存主题、显示已完成、通知权限提示标记 |
| AlarmManager + Notification | `ReminderScheduler` / `ReminderReceiver` / `TodoApp` |
| Material Design | Material 3 主题、Chip、TextInputLayout、FAB、Snackbar、MaterialCardView |

## 构建

- JDK 17
- Android SDK Platform 35
- minSdk 27（Android 8.1）
- Gradle 8.13（已带 wrapper）

```bash
gradlew assembleDebug
```

仓库中附带两个纯 Java 单元测试（日期格式化、JSON 导出）：

```bash
gradlew testDebugUnitTest
```

生成的 APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。

> 项目根目录的 `local.properties` 指向本机 SDK 路径，换机器后由 Android Studio 重新生成即可。
