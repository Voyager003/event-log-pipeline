## 이벤트 로그 파이프라인 구축

`D2C 비즈니스`를 서비스한다고 가정하고 이벤트 발생, 이벤트 로그 생성/저장 및 분석 그리고 시각화한 프로젝트입니다.

### 실행 방법

1. 필요한 도구

- Docker Desktop 또는 Docker Engine
- Docker Compose

2. 설치 명령어

```bash
# Docker Desktop 설치 (for macOS)
brew install --cask docker

git clone https://github.com/<your-org>/<your-repo>.git
cd event-log-pipeline
```

3. 실행 명령어

```bash
docker compose up --build
```

## Step 1. 이벤트 생성기 작성

### 이벤트 설계

이벤트 설계를 위한 시나리오를 하나 정의했습니다.

> 어떤 시간대에 사람들이 가장 많이 강의를 수강하는지 파악하여 해당 시간대에 유동적으로 Auto Scaling을 적용해야 한다.

이 시나리오를 확인하기 위해 강의 재생 활동량, 활성 수강 세션 수, 영상 재생 오류율을 집계할 수 있는 이벤트를 구성했습니다.

| 이벤트 타입 | Source | 의미 | 선택 이유                                     |
| --- | --- | --- |-------------------------------------------|
| `lecture_started` | `WEB` | 사용자가 강의 재생을 시작 | 수강 세션 시작과 시간대별 수강 시작량을 보기 위함 |
| `lecture_playback_heartbeat` | `WEB` | 사용자가 강의를 재생 중일 때 주기적으로 발생 | 실제 수강 중인 세션 수와 피크 시간대를 보기 위함 |
| `lecture_completed` | `WEB` | 사용자가 강의를 완료 | 수강 완료 이벤트까지 포함한 전체 강의 소비량을 보기 위함 |
| `video_error_occurred` | `SERVER` | 영상 재생 오류가 발생 | 트래픽 집중 시간대에 오류율이 증가하는지 보기 위함 |

실제 사용자가 없는 과제 환경이므로 이벤트 생성기가 웹/서버에서 발생하는 사용자 행동을 대신 생성합니다.

이 때, 특정 시간대 결과를 의도적으로 만들지 않기 위해 수강 세션의 시작 시각, 사용자, 강의, 기기, 시청 시간, 종료 상태를 난수로 생성합니다.

단, 같은 세션 안에서는 `lecture_started -> lecture_playback_heartbeat -> lecture_completed`, `video_error_occurred` 순서를 보장하도록 설계했습니다.

이를 고려하지 않고 난수로 이벤트를 생성하게 되면 사용자가 강의 재생을 시작하는 시간보다 강의를 완료하는 시간이 앞에 위치하게 되어 의미없는 데이터를 생성할 수 있기 때문입니다.

최종 이벤트 타입과 필드 구성은 다음과 같습니다.

| 이벤트 타입 | 공통 필드 | 상세 필드 |
| --- | --- | --- |
| `lecture_started` | `eventId`, `eventType`, `source`, `occurredAt`, `userId`, `anonymousId`, `sessionId`, `deviceType` | `courseId`, `lectureId`, `playbackPositionSeconds`, `watchDurationSeconds`, `completionRate` |
| `lecture_playback_heartbeat` | `eventId`, `eventType`, `source`, `occurredAt`, `userId`, `anonymousId`, `sessionId`, `deviceType` | `courseId`, `lectureId`, `playbackPositionSeconds`, `watchDurationSeconds`, `completionRate` |
| `lecture_completed` | `eventId`, `eventType`, `source`, `occurredAt`, `userId`, `anonymousId`, `sessionId`, `deviceType` | `courseId`, `lectureId`, `playbackPositionSeconds`, `watchDurationSeconds`, `completionRate` |
| `video_error_occurred` | `eventId`, `eventType`, `source`, `occurredAt`, `userId`, `anonymousId`, `sessionId`, `deviceType` | `courseId`, `lectureId`, `errorType`, `errorCode`, `errorMessage` |

## Step 2. 로그 저장

### 스키마 구조

![](/img/01.png)

`event_logs`는 부모 테이블, `lecture_event_details`, `video_error_event_details` 는 이벤트 상세 정보를 담은 자식 테이블입니다.

이벤트 타입이나 유저/세션 id는 언제, 어떤 기기에서 이벤트가 발생했는지 추적하기 위해 공통적으로 필요한 값입니다.

그래서 상세 정보를 담은 필드와 공통 필드를 분리하여 `event_id`로 JOIN하도록 하나의 이벤트로 구성했습니다.

### 저장소 선택

로그 저장소는 RDB인 `PostgreSQL`을 사용했습니다.

`파일 기반 저장`은 대규모 로그를 저비용으로 빠르게 적재하고 장기 보관하기에 유리하지만, 즉시 JOIN과 정합성 관리가 상대적으로 약합니다.

반면 `데이터베이스 기반`은 공통 필드와 상세 필드를 event_id로 연결해 집계와 시각화를 바로 수행하기 좋지만, 대규모 적재와 장기 보관에서는 파티션이나 별도 저장소 확장이 필요하다는 특징이 있습니다.

현재 프로젝트는 로그의 저장 규모와 이벤트 생성, 필드 분리 저장, SQL 집계, Grafana 시각화 작업까지 고려했을 때 RDB가 더 적합하다고 판단하여 PostgreSQL에 로그를 저장했습니다.

## Step 3. 데이터 집계 분석

현재 시나리오는 어떤 시간대에 사람들이 가장 많이 강의를 수강하는지 파악하는 것입니다. 

쿼리는 다음과 같습니다.

- `hourly_lecture_event_trends.sql`

시간대별 강의 재생 heartbeat 수를 집계합니다. 사용자가 실제로 강의를 재생 중인 시간대를 확인하고, Auto Scaling이 필요한 피크 구간이 있는지 검증하기 위한 쿼리입니다.

- `hourly_active_sessions.sql`

- 시간대별 활성 수강 세션 수와 활성 수강 사용자 수를 집계합니다. 특정 시간대에 몇 명이 실제로 강의를 보고 있었는지 확인하고, 동시 수강 규모가 어느 정도인지 검증하기 위한 쿼리입니다.

- `hourly_video_error_rate.sql`

시간대별 재생 heartbeat 수, 오류 이벤트 수, 오류율을 집계합니다. 트래픽이 몰리는 시간대에 영상 재생 오류가 함께 증가하는지 확인하고, 부하 증가와 품질 저하가 함께 나타나는지 검증하기 위한 쿼리입니다.

## Step 5. 결과 시각화

![](/img/03.png)
![](/img/04.png)
![](/img/05.png)

## AWS 아키텍처 설계



## 참고 자료

- [사용자 행동 로그, 파종부터 수확까지](https://medium.com/@connect2yh/%EC%82%AC%EC%9A%A9%EC%9E%90-%ED%96%89%EB%8F%99-%EB%A1%9C%EA%B7%B8-%ED%8C%8C%EC%A2%85%EB%B6%80%ED%84%B0-%EC%88%98%ED%99%95%EA%B9%8C%EC%A7%80-%EC%9E%98-%EC%8C%93%EA%B8%B0-1%ED%8E%B8-9733422dd00d)
- [데이터 로그](https://zzsza.github.io/data/2021/06/13/data-event-log-definition/)
