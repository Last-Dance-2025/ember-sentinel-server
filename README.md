# Ember Sentinel API Server

> INHA Univ. 캡스톤 디자인 - Ember Sentinel 프로젝트의 API 서버용 레포지토리입니다.

본 프로젝트는 Edge AI 카메라를 활용한 조기 화재 및 연기 감지 서비스의 벡엔드 API 서버입니다. 실시간 비디오 스트리밍, 화재 이벤트 기록, 실시간 모바일 푸시 알림 기능을 지원합니다.

## 핵심 기능

- **도메인 관리**: 건물(Building), 방(Room), 카메라 엣지 장치(CameraEdge) 등록 및 구조적 관리
- **실시간 스트리밍 및 화재 녹화 (LiveKit & AWS S3)**: WebRTC 기반의 실시간 현장 영상 스트리밍을 제공하며, 화재 감지 시 자동으로 영상을 AWS S3에 녹화(Egress) 및 저장
- **실시간 알림 (Firebase Cloud Messaging)**: 화재/연기 감지 이벤트 발생 시 해당 방 관리자 및 유저들에게 실시간 모바일 푸시 알림 전송
- **사용자 인증 및 권한**: JWT(JSON Web Token)를 활용한 사용자 인증 및 역할(Role) 기반의 API 접근 제어

## 기술 스택

- **Backend Framework**: Java 17, Spring Boot 3.x, Spring Data JPA
- **Database & Cache**: 
  - PostgreSQL (건물/방/카메라 등 구조화된 도메인의 무결성 관리 및 화재 이벤트 영상 메타데이터의 안정적인 영구 저장)
  - Redis (사용자 인증 토큰 관리 및 비동기 형태의 FCM 푸시 알림 데이터의 빠른 I/O 처리를 위한 인메모리 캐싱)
- **Third-party Services**: 
  - LiveKit (WebRTC 스트리밍 및 Egress 녹화)
  - Firebase Cloud Messaging (FCM 실시간 푸시 알림)
  - AWS S3 (영상 녹화본 저장 및 Presigned URL 제공)
- **Security & API Docs**: JWT, Springdoc OpenAPI (Swagger UI)

## API 명세서

- [노션 API 명세서 페이지](https://www.notion.so/API-28cf88309df2801fb4ccc30c6681b5b8?source=copy_link)