# Android App Launch Benchmark

```angular2html
本README.md由GPT-6生成
```

这是一个运行在主机端的 Kotlin/JVM 命令行工具，用于在不同 Android 内存交换方案下，连续启动一组应用并采集：

- App 冷、温、热启动状态及 `am start -W` 延迟
- 帧卡顿比例和渲染延迟分位数
- App PSS 内存
- `/proc/meminfo`、memory PSI 和 `vmstat`
- 设备温度
- zRAM、backing device 和 zSwap 指标
- LMKD 杀进程日志
- 每轮结束时各 App 的进程存活状态

项目适合比较 No Swap、zRAM、Flash Swap 和 zSwap 等方案在连续多应用负载下的表现。

## 工作流程

一次运行会依次执行：

1. 重启连接的 Android 设备并等待 `sys.boot_completed=1`。
2. 唤醒屏幕并尝试解除非安全锁屏。
3. 解析测试参数，创建结果目录。
4. 根据参数配置 Swap 方案。
5. 使用给定随机种子将应用列表打乱一次。
6. 启动指标采样任务和 LMKD logcat 采集。
7. 连续运行 5 轮应用启动测试。
8. 每轮结束后检查本轮各 App 的原 PID 是否仍然存活。
9. 输出 CSV、原始日志和汇总结果。

当前默认时间配置为：

| 配置 | 默认值 |
|---|---:|
| 测试轮数 | 5 |
| 指标采样间隔 | 10 秒 |
| 相邻 App 启动间隔 | 15 秒 |
| 启动后 gfxinfo/meminfo 收集时刻 | 12 秒 |
| Flash Swap 文件大小 | 4096 MB |

这些默认值定义在 [`ExperimentConfig.kt`](app/src/main/kotlin/com/junj/config/ExperimentConfig.kt)。

## 环境要求

### 主机

- Linux 或 WSL 环境
- JDK 21
- 项目自带 Gradle Wrapper
- 可从命令行执行 `adb.exe`

当前代码明确调用的是 `adb.exe`，适合在 WSL 中复用 Windows Android Platform Tools。如果在原生 Linux 上运行，需要将 [`AdbCommands.kt`](app/src/main/kotlin/com/junj/device/adb/AdbCommands.kt) 和 [`LogcatProcess.kt`](app/src/main/kotlin/com/junj/device/logcat/LogcatProcess.kt) 中的 `adb.exe` 改为 `adb`。

### Android 设备

- 已通过 ADB 连接且处于 authorized 状态
- 设备具有 root 权限
- `adb shell su -c sh` 可用
- 锁屏不使用 PIN、密码或图案，或者在运行前手动解锁
- 被测应用已经安装，包名和启动 Activity 与应用目录一致
- 测试 zSwap 时，内核需要支持 zSwap 和 debugfs
- 测试 Flash Swap 时，需要允许在 `/data/per_boot/` 创建 swap 文件

可以先检查：

```bash
adb.exe devices
adb.exe shell su -c id
adb.exe shell pm list packages
```

> [!WARNING]
> 每次运行都会先重启设备，并可能执行 `swapoff`、创建 4 GB 的 `/data/per_boot/flash.swap`、执行 `mkswap`/`swapon` 和修改 zSwap 参数。请只在专用测试设备上运行。

## 构建和测试

编译：

```bash
./gradlew :app:compileKotlin
```

运行单元测试：

```bash
./gradlew :app:test
```

## 运行测试

基本命令：

```bash
./gradlew run --args="-t <swap-type> -a <app-set>"
```

示例：使用 zRAM、中等应用集合和随机种子 42：

```bash
./gradlew run --args="-t 1 -a 1 --seed 42"
```

为输出目录增加标识：

```bash
./gradlew run --args="-t 3 -a 2 --seed 42 --logPath run01"
```

参数说明：

| 参数 | 必填 | 默认值 | 说明 |
|---|---|---:|---|
| `-t`, `--type` | 是 | — | Swap 方案，取值为 0～3 |
| `-a`, `--app-set` | 是 | — | 应用集合，取值为 0～2 |
| `-s`, `--seed` | 否 | 0 | 应用启动顺序的随机种子 |
| `-p`, `--logPath` | 否 | — | 附加到输出目录名末尾的标识 |

`--logPath` 虽然名称中包含 path，但目前并不是自定义文件系统路径，只是输出目录名后缀。

> [!IMPORTANT]
> 当前程序在解析命令行参数之前就会重启设备。因此，即使参数无效或只传入 `--help`，设备也可能先被重启。

### Swap 类型

| 值 | 模式 | 当前操作 |
|---:|---|---|
| 0 | No Swap | 对 `/dev/block/zram0` 执行 `swapoff` |
| 1 | Only zRAM | 保持设备当前 zRAM 配置 |
| 2 | Flash Swap | 关闭 zRAM，创建并启用 `/data/per_boot/flash.swap` |
| 3 | zSwap | 关闭 zRAM，启用 Flash Swap 和 zSwap，并挂载 debugfs |

程序目前不会完整清理上一次运行遗留的所有 Swap/zSwap 状态。进行严格对比实验时，建议每个独立实验都从已知设备镜像和配置开始，并检查 `/proc/swaps` 与 zSwap 参数。

### 应用集合

应用集合定义在 [`AppCatalog.kt`](app/src/main/kotlin/com/junj/domain/app/AppCatalog.kt)：

| 值 | 名称 | 数量 | 应用 |
|---:|---|---:|---|
| 0 | Light | 10 | 微信、微博、知乎、QQ 音乐、大众点评、携程、京东、美团、百度网盘、夸克 |
| 1 | Middle | 12 | Light 的大部分应用，加 WPS、豆包、高德地图 |
| 2 | Heavy | 16 | 当前目录中的全部应用，包括抖音、哔哩哔哩和快手 |

实际包名和 Activity 也在该文件中维护。设备上的应用版本更新后，如果启动 Activity 发生变化，需要同步修改 `componentName`。

### 随机种子

程序在所有测试轮次开始前只打乱一次应用列表。同一次运行中的各轮使用同一个顺序。

```bash
# 两次运行会获得相同的初始顺序
./gradlew run --args="-t 0 -a 0 --seed 100"
./gradlew run --args="-t 1 -a 0 --seed 100"
```

比较不同 Swap 方案时应使用相同 seed，使各方案面对相同应用顺序。最终顺序会写入 `summary.txt`：

```text
randomSeed=100, appOrder=wechat, zhihu, ...
```

## 输出结果

结果默认写入仓库根目录的 `logs/`：

```text
logs/
└── 20261010_120000_light_onlyZram_run01/
    ├── launch.csv
    ├── launch.log
    ├── logcat.log
    ├── sample.csv
    ├── sample.log
    ├── summary.txt
    └── survival.csv
```

### `launch.csv`

每次 App 启动一行，包含：

- 轮次、应用名和启动状态
- `TotalTime`、`WaitTime` 和启动状态
- Janky frames 百分比
- P50、P90、P95、P99 渲染延迟
- Total PSS

### `sample.csv`

每次系统采样一行，包含：

- 内存、Swap 和 memory PSI
- `vmstat` 进程、内存、I/O 和 CPU 指标
- CPU、SoC 和机身温度
- zRAM 压缩及 backing device 统计
- zRAM backing device 和 zSwap 写回带宽

### `survival.csv`

每轮每个 App 一行：

| 字段 | 含义 |
|---|---|
| `round` | 测试轮次 |
| `launchPosition` | App 在本轮中的启动位置，从 1 开始 |
| `appName` | 应用简称 |
| `packageName` | Android 包名 |
| `launchedPid` | 本次启动后捕获的主进程 PID；获取失败时为空 |
| `pidAtRoundEnd` | 本轮结束时 `pidof` 返回的主进程 PID；不存在时为空 |
| `originalProcessAlive` | 启动时捕获的原 PID 在轮末是否仍属于该包 |
| `appCurrentlyRunning` | 本轮结束时是否仍能找到该应用主进程 |

两个存活字段用于区分：

- `originalProcessAlive=true`：原启动进程仍然存活。
- `originalProcessAlive=false, appCurrentlyRunning=true`：原进程已死亡，但应用可能被重新拉起。
- 两者均为 `false`：轮末没有发现应用主进程。

### `summary.txt`

汇总内容包括：

- 实际随机种子和应用顺序
- 冷、温、热及未知启动次数
- 平均启动响应延迟
- LMKD 日志行数
- 原进程存活率和应用当前运行率
- 平均温度
- 测试期间 PSI 增量
- 整体运行时间

`lmkdCnt` 当前是 `logcat.log` 中匹配 `lowmemorykiller: kill` 的行数，不一定等于经过 PID 去重后的唯一杀进程次数。

## 批量运行

根目录的 [`test.sh`](test.sh) 会依次运行 4 种 Swap 类型和 3 种应用集合，共 12 次实验：

```bash
bash test.sh
```

每次实验都会重启设备，完整批量运行耗时较长。为保证对比公平，建议为脚本中的各组实验显式传入相同 seed，并在不同方案之间确认设备温度和 Swap 状态。

## 项目结构

```text
app/src/main/kotlin/com/junj/
├── Main.kt                 # 依赖组装和实验生命周期
├── cli/                    # 命令行参数
├── config/                 # 实验配置和默认值
├── device/                 # ADB、设备重启、Swap、PID、logcat 和 UI 操作
├── domain/                 # 应用、启动、采样和存活结果模型
├── experiment/             # 应用启动及轮次执行
├── metrics/                # 指标采集任务与解析器
└── output/                 # CSV、日志和汇总输出
```

主要依赖：

- Kotlin/JVM
- Kotlin Coroutines
- Clikt
- JUnit 5 / Kotlin Test

## 自定义测试

### 修改应用列表

编辑 [`AppCatalog.kt`](app/src/main/kotlin/com/junj/domain/app/AppCatalog.kt)，为应用配置：

```kotlin
ApplicationInfo(
    name = "example",
    packageName = "com.example.app",
    componentName = "com.example.app/.MainActivity",
)
```

并将 `name` 加入所需的 `appNameSet`。

可通过下面的命令确认 Activity：

```bash
adb.exe shell cmd package resolve-activity --brief com.example.app
```

### 修改实验时间和轮数

编辑 [`ExperimentConfig.kt`](app/src/main/kotlin/com/junj/config/ExperimentConfig.kt) 中的默认常量：

```kotlin
const val DEFAULT_TEST_ROUND_COUNT = 5
const val DEFAULT_SAMPLE_INTERVAL_MS = 10_000L
const val DEFAULT_LAUNCH_INTERVAL_MS = 15_000L
const val DEFAULT_COLLECT_DELAY_MS = 12_000L
```

其中 `collectDelayMs` 必须小于或等于 `launchIntervalMs`，否则实际启动间隔会超过配置值。

## 已知限制

- 当前只支持单个默认 ADB 设备，没有 `-s <serial>` 参数。
- ADB stderr 与 stdout 合并，部分命令失败可能表现为解析结果为 0。
- 部分采样字段在读取或解析失败时回退为 0，可能与真实零值混淆。
- 指标采样周期是“采集耗时 + 配置间隔”，不是严格的固定时间点。
- 应用启动顺序会影响轮末存活率；较早启动的应用承受内存压力的时间更长。
- 5 个 round 属于同一次连续工作负载，不是 5 个独立重启样本。
- `Only zRAM` 模式依赖设备启动后的既有 zRAM 配置。
- UI 自动操作代码尚未接入默认测试流程。

## 结果对比建议

- 不同 Swap 方案使用相同应用集合、seed 和应用版本。
- 将一次设备重启后的完整执行视为一个 run，将内部循环视为 round。
- 使用多个 seed 重复完整 run，不要把同一 run 内的 round 当作独立样本。
- 同时分析启动延迟、原进程存活率、PSI、温度和 Swap I/O，不要只看单一指标。
- 测试前固定电量、充电状态、屏幕亮度、网络、应用登录状态及设备温度。
