# zrlog-plugin-mail

ZrLog 邮件服务插件。当前版本保留旧 `emailService` 服务入口，同时通过 `notification.email.send` 暴露为 v4 插件运行时的标准 `email` 通知通道。

```shell
export JAVA_HOME=${HOME}/dev/graalvm-jdk-latest
export PATH=${JAVA_HOME}/bin:$PATH
```
