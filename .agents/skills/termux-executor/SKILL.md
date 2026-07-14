---
name: termux-executor
description: 使用此技能在已连接设备的 Termux 终端中执行 shell 命令，并同步获取截图和控制台日志输出。适用于需要调试、测试、在真机/模拟器前台直观展现命令执行结果的开发场景。
---

# Termux 命令行执行器技能

## 目标
方便地在已连接的 Android 设备的 Termux 应用前台运行任意 Shell 命令，同时以截图形式向用户和 AI 展示直观的运行界面，并以日志文本形式拉取准确的输出。

## 操作指南
1. 在 Windows 命令行或通过 `run_command` 工具运行位于项目根目录下的 `run_on_termux.ps1` 脚本：
   ```powershell
   powershell -ExecutionPolicy Bypass -File .\run_on_termux.ps1 -Command "你的 Shell 命令"
   ```
2. 脚本将会自动执行以下步骤：
   - 将命令封装后通过 `adb shell run-as com.termux` 传输至设备内部。
   - 检查设备屏幕状态，若为休眠或低功耗显示状态则自动唤醒并尝试上滑解锁。
   - 自动将 Termux 应用置于前台。
   - 模拟键盘输入并触发回车，让命令在 Termux 终端界面前台执行，以便用户直接看到执行过程。
   - 等待命令执行完毕并自动截图，将图片保存到本地 artifacts 路径。
   - 读取并输出完整的命令标准输出与标准错误日志。
3. 运行完成后，你（AI）应该使用 `view_file` 查看生成的截图以确认前台显示状态，并把结果呈现给用户。

## 常用命令示例
- 查询网络配置：
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\run_on_termux.ps1 -Command "ifconfig"
  ```
- 检查 Termux 安装的包列表：
  ```powershell
  powershell -ExecutionPolicy Bypass -File .\run_on_termux.ps1 -Command "pkg list-installed"
  ```
