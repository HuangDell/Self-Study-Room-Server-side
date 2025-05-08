# 使用 Maven 镜像作为基础镜像
FROM fnil-dm2.fnil.ac.cn/maven:3.8.2-openjdk-17-slim AS build

# 设置工作目录
WORKDIR /usr/src/app

# 复制 pom.xml 文件
COPY pom.xml .

# 下载依赖项
RUN mvn dependency:go-offline

# 复制其他源代码文件
COPY src src

# 构建项目
RUN mvn clean package -Dmaven.test.skip=true -DskipTests

# 使用 Java 镜像作为基础镜像
FROM fnil-dm2.fnil.ac.cn/maven:3.9-eclipse-temurin-17
RUN echo $JAVA_HOME

# 设置工作目录
WORKDIR /app

RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone
# 从构建阶段复制构建的 jar 文件
COPY --from=build /usr/src/app/target/SelfStudyRoom-1.0.jar /app

# 设置启动命令
CMD ["java", "-Xmx200m", "-jar", "/app/SelfStudyRoom-1.0.jar"]

EXPOSE 8080