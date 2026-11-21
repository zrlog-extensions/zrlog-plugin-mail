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

## 原生制品发布

Linux amd64/arm64 制品在上传前会调用 `zrlog-artifact-service`，通过与 `plugin-core`
相同的固定发布版 [`zrlog-build`](https://github.com/zrlog/zrlog-build)
Action（`v1.0.1`，工作流固定完整 SHA）完成压缩和 SHA-256、文件大小校验；接入与故障处理见该仓库 README。
处理成功后才会生成最终制品的 MD5 并上传；处理失败会停止该平台的发布。
服务接收的版本号来自 `zrlog-build` 构建脚本生成的 `plugin.properties`。

发布前需要配置 Actions Secret `ARTIFACT_SERVICE_TOKEN`，可在仓库中单独设置，
或授权该仓库使用同名组织 Secret。在仓库或组织的 Actions Variables 中配置服务地址 `ARTIFACT_SERVICE_URL`。
