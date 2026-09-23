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
相同的固定版本 `process-artifact` Action 完成压缩和 SHA-256、文件大小校验。
处理成功后才会生成最终制品的 MD5 并上传；处理失败会停止该平台的发布。
服务接收的版本号使用 `bin/build-info.sh` 生成的实际插件版本。

发布前需要配置 Actions Secret `ARTIFACT_SERVICE_TOKEN`，可在仓库中单独设置，
或授权该仓库使用同名组织 Secret。服务地址为 `https://webdav.zrlog.com/artifact`。
