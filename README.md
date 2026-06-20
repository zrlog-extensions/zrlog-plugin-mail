# zrlog-plugin-email

ZrLog 邮件服务插件。通过 SMTP 发送系统通知邮件，并记录最近的发送结果。当前版本兼容旧 `emailService` 调用，同时作为 `email` 通知通道接收系统通知。

## 功能

- 配置 SMTP 服务器、端口、发件邮箱和默认收件邮箱
- 发送测试邮件
- 接收其他插件的邮件通知调用
- 记录发送状态、收件人、来源和错误信息

## 构建

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```
