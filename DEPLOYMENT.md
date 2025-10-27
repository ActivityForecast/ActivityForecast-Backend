# ActivityForecast Backend 배포 가이드

## 개요

이 문서는 ActivityForecast Backend를 Oracle Cloud 인스턴스에 Docker와 GitHub Actions를 이용하여 자동 배포하는 방법을 설명합니다.

## 시스템 아키텍처

```
GitHub Repository → GitHub Actions → Docker Hub → Oracle Cloud Instance
                                                  ├── Nginx (리버스 프록시, SSL)
                                                  ├── Spring Boot App
                                                  ├── MySQL Database
                                                  └── Redis Cache
```

## 사전 준비사항

### 1. Oracle Cloud 인스턴스 설정

#### 인스턴스 생성
- **OS**: Ubuntu 22.04 LTS
- **Shape**: VM.Standard.E2.1.Micro (무료 티어)
- **Storage**: 50GB Boot Volume

#### 보안 그룹 설정
```bash
# Ingress Rules 추가
Port 22 (SSH) - 0.0.0.0/0
Port 80 (HTTP) - 0.0.0.0/0  
Port 443 (HTTPS) - 0.0.0.0/0
Port 8080 (Spring Boot) - 0.0.0.0/0 (선택사항)
```

#### 필수 소프트웨어 설치
```bash
# 시스템 업데이트
sudo apt update && sudo apt upgrade -y

# Docker 설치
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo usermod -aG docker $USER

# Docker Compose 설치
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# Git 설치
sudo apt install git -y

# 기타 유틸리티
sudo apt install curl wget unzip -y
```

### 2. GitHub 저장소 설정

#### SSH 키 생성 및 등록
```bash
# Oracle 인스턴스에서 SSH 키 생성
ssh-keygen -t rsa -b 4096 -C "your-email@example.com"

# 공개키를 GitHub에 Deploy Key로 등록
cat ~/.ssh/id_rsa.pub
```

#### 프로젝트 클론
```bash
# 프로젝트 디렉토리 생성
sudo mkdir -p /opt/activityforecast
sudo chown $USER:$USER /opt/activityforecast

# 저장소 클론
cd /opt/activityforecast
git clone https://github.com/your-username/ActivityForecast-Backend.git .
```

### 3. Docker Hub 계정 설정

1. [Docker Hub](https://hub.docker.com) 계정 생성
2. 새 Repository 생성: `your-username/activityforecast-backend`
3. Access Token 생성 (Settings → Security)

## 환경 설정

### 1. 환경 변수 파일 생성

```bash
# .env 파일 생성
cp .env.example .env
nano .env
```

`.env` 파일 내용:
```env
# Database Configuration
MYSQL_ROOT_PASSWORD=your_secure_root_password_here
MYSQL_DATABASE=activity_forecast
MYSQL_USER=activityuser
MYSQL_PASSWORD=your_secure_user_password_here

# JWT Configuration  
JWT_SECRET=your_jwt_secret_key_minimum_32_characters_long_here

# External API Keys
WEATHER_API_KEY=your_openweathermap_api_key_here
AIR_QUALITY_API_KEY=your_airvisual_api_key_here

# Social Login
KAKAO_CLIENT_ID=your_kakao_client_id_here
KAKAO_CLIENT_SECRET=your_kakao_client_secret_here

# Redis
REDIS_PASSWORD=your_redis_password_here

# CORS
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

### 2. GitHub Secrets 설정

GitHub 저장소의 Settings → Secrets and variables → Actions에서 다음 secrets 추가:

```
# Docker Hub
DOCKER_USERNAME=your_dockerhub_username
DOCKER_PASSWORD=your_dockerhub_access_token

# Oracle Cloud Instance
ORACLE_HOST=your_oracle_instance_public_ip
ORACLE_USER=ubuntu
ORACLE_PORT=22
ORACLE_SSH_KEY=your_private_key_content

# Application Environment Variables
MYSQL_ROOT_PASSWORD=your_secure_root_password
MYSQL_DATABASE=activity_forecast
MYSQL_USER=activityuser
MYSQL_PASSWORD=your_secure_user_password
JWT_SECRET=your_jwt_secret_key_minimum_32_characters
WEATHER_API_KEY=your_openweathermap_api_key
AIR_QUALITY_API_KEY=your_airvisual_api_key
KAKAO_CLIENT_ID=your_kakao_client_id
KAKAO_CLIENT_SECRET=your_kakao_client_secret
REDIS_PASSWORD=your_redis_password
CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
```

## 배포 프로세스

### 1. 수동 배포 (초기 설정)

```bash
# Oracle 인스턴스에서 실행
cd /opt/activityforecast

# 환경 변수 설정 확인
source .env

# 초기 배포 실행
chmod +x scripts/deploy.sh
./scripts/deploy.sh
```

### 2. 자동 배포 (GitHub Actions)

1. **코드 변경사항 커밋**
```bash
git add .
git commit -m "feat: 새로운 기능 추가"
git push origin main
```

2. **GitHub Actions 워크플로우 자동 실행**
   - 테스트 실행
   - Docker 이미지 빌드 및 푸시
   - Oracle Cloud 인스턴스에 자동 배포
   - 헬스체크 수행

3. **배포 상태 확인**
   - GitHub Actions 탭에서 배포 진행상황 확인
   - 성공 시 애플리케이션 접속 가능

## 서비스 접근

### 애플리케이션 URL
- **메인 API**: `https://your-domain.com/api`
- **Swagger UI**: `https://your-domain.com/api/swagger-ui.html`
- **헬스체크**: `https://your-domain.com/api/test/health`

### 로컬에서 테스트
```bash
# 개발 환경 실행
docker-compose up -d

# 로컬 접근
http://localhost:8080/api/swagger-ui.html
```

## SSL/HTTPS 설정

### Let's Encrypt 인증서 설정 (권장)

```bash
# Certbot 설치
sudo apt install certbot python3-certbot-nginx -y

# 인증서 발급 (도메인이 있는 경우)
sudo certbot --nginx -d your-domain.com -d www.your-domain.com

# 자동 갱신 설정
sudo crontab -e
# 다음 라인 추가:
# 0 12 * * * /usr/bin/certbot renew --quiet
```

### 자체 서명 인증서 (개발용)
배포 스크립트가 자동으로 자체 서명 인증서를 생성합니다.

## 모니터링 및 로그

### 로그 확인
```bash
# 애플리케이션 로그
docker-compose -f docker-compose.prod.yml logs -f app

# Nginx 로그  
docker-compose -f docker-compose.prod.yml logs -f nginx

# 모든 서비스 로그
docker-compose -f docker-compose.prod.yml logs -f
```

### 서비스 상태 확인
```bash
# 컨테이너 상태
docker-compose -f docker-compose.prod.yml ps

# 시스템 리소스
docker stats

# 디스크 사용량
df -h
```

## 문제 해결

### 일반적인 문제들

1. **컨테이너가 시작되지 않는 경우**
```bash
# 로그 확인
docker-compose -f docker-compose.prod.yml logs app

# 환경변수 확인
docker-compose -f docker-compose.prod.yml config
```

2. **데이터베이스 연결 오류**
```bash
# MySQL 컨테이너 상태 확인
docker-compose -f docker-compose.prod.yml exec mysql mysqladmin ping

# 연결 테스트
docker-compose -f docker-compose.prod.yml exec mysql mysql -u $MYSQL_USER -p$MYSQL_PASSWORD -e "SELECT 1;"
```

3. **SSL 인증서 문제**
```bash
# 인증서 확인
openssl x509 -in ssl/cert.pem -text -noout

# 인증서 재생성
rm ssl/cert.pem ssl/key.pem
./scripts/deploy.sh
```

### 서비스 재시작
```bash
# 전체 서비스 재시작
docker-compose -f docker-compose.prod.yml restart

# 특정 서비스만 재시작
docker-compose -f docker-compose.prod.yml restart app
```

## 백업 및 복구

### 데이터베이스 백업
```bash
# 수동 백업
docker exec activityforecast-mysql mysqldump -u root -p$MYSQL_ROOT_PASSWORD --all-databases > backup_$(date +%Y%m%d_%H%M%S).sql

# 자동 백업 스크립트 (cron)
0 2 * * * /opt/activityforecast/scripts/backup.sh
```

### 데이터베이스 복구
```bash
# 백업에서 복구
docker exec -i activityforecast-mysql mysql -u root -p$MYSQL_ROOT_PASSWORD < backup_file.sql
```

## 업데이트 및 배포

### 새 버전 배포
1. 코드 변경 후 main 브랜치에 푸시
2. GitHub Actions가 자동으로 배포 실행
3. 배포 완료 후 헬스체크로 서비스 상태 확인

### 롤백
```bash
# 이전 Docker 이미지로 롤백
docker tag your-username/activityforecast-backend:previous your-username/activityforecast-backend:latest
./scripts/deploy.sh
```

## 보안 고려사항

1. **정기적인 시스템 업데이트**
2. **강력한 비밀번호 사용**
3. **불필요한 포트 차단**
4. **SSL 인증서 갱신**
5. **로그 모니터링**
6. **백업 정기 수행**

## 지원 및 문의

문제가 발생하거나 도움이 필요한 경우:
- GitHub Issues: [프로젝트 이슈 페이지]
- Email: dev@activityforecast.com