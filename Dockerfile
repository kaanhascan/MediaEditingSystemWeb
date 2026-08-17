# 1. Base Image: Java 21 (Ubuntu 'jammy' tabanlı, FFmpeg kurulumu için ideal)
FROM eclipse-temurin:21-jdk-jammy

# 2. Sistemi güncelle ve FFmpeg'i kur (Sıfır RAM ile indirme/kesme yapabilmek için gerekli)
RUN apt-get update && \
    apt-get install -y ffmpeg && \
    apt-get clean

# 3. Uygulamanın çalışacağı ana dizini belirle
WORKDIR /app

# 4. Video yüklemeleri ve kesme işlemleri için gerekli klasörleri oluştur (Linux izin sorunlarını önler)
RUN mkdir -p /app/uploads/processed

# 5. Projeyi derledikten sonra oluşan .jar dosyasını kopyala
COPY target/*.jar app.jar

# 6. Dışarıya açılacak port (Spring Boot varsayılan olarak 8080 kullanır)
EXPOSE 8080

# 7. Konteyner ayağa kalktığında çalıştırılacak komut
ENTRYPOINT ["java", "-jar", "app.jar"]