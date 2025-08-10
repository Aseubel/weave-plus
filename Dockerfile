# 使用一个只包含 JRE 的轻量级基础镜像
FROM eclipse-temurin:17-jre-focal

# 设置工作目录
WORKDIR /app

# 将工作流中已经构建好的 JAR 文件复制到镜像中
# 注意：这里的路径需要与 `weave-user-app` 模块的 `pom.xml` 中定义的 finalName 匹配
COPY weave-service/weave-user-service/weave-user-app/target/weave-user-app.jar app.jar

# 暴露应用端口
EXPOSE 20611

# 运行应用的命令
ENTRYPOINT ["java","-jar","/app/app.jar"]