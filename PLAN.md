# Implementation Plan - PulseMetrics: Real-Time Analytics Platform

This plan outlines the design and implementation of **PulseMetrics**, an event-driven analytics platform. We will build a fully functional system in `Pulse-Metrics/` containing a Spring Boot backend, a Vite + React frontend, a Docker-compose environment for infra dependencies (PostgreSQL, Kafka, Redis), and an event load simulator to demo the system's real-time capabilities.

---

## User Review Required

> [!IMPORTANT]
> The project will be set up as a multi-module workspace in `/Users/akhilesh/Documents/project/Pulse-Metrics` to keep the backend, frontend, and infrastructure isolated but easy to manage.

> [!TIP]
> To enable local development and testing, we will run Kafka, PostgreSQL, and Redis in Docker containers. Ensure Docker Desktop is installed and running on your system.

## Open Questions

> [!IMPORTANT]
> Please review and provide feedback on the following implementation options before we start writing the code:
>
> 1. **Framework Choices**: Do you want both a Vite + React frontend dashboard and a Spring Boot backend, or would you prefer a backend-only API with CLI/simulated outputs?
> 2. **AI Chat Interface**: Would you like to include a Gemini-powered chat interface in the dashboard to enable natural language queries against your metrics (e.g., "Are there any anomalies today?")?
> 3. **Aggregation Layer**: Should we use standard Spring Kafka consumers with scheduled sliding window aggregation, or would you like to see Kafka Streams used for event time windowing?
> 4. **Alert Notifications**: For alerts, should we mock email deliveries in the logs and send actual live alerts to the UI via Server-Sent Events (SSE) / WebSockets?

---

## Proposed Changes

We will construct the project inside the `/Users/akhilesh/Documents/project/Pulse-Metrics` directory with the following structure:

```
Pulse-Metrics/
├── docker-compose.yml                   # Postgres, Kafka, Zookeeper, Redis
├── init.sql                             # SQL schema with partition configuration
├── PLAN.md                              # This plan file
├── pulse-metrics-backend/               # Spring Boot Application
│   ├── pom.xml                          # Maven dependencies (Kafka, Redis, Web, Security, JPA)
│   └── src/main/java/com/pulse/metrics/
│       ├── PulseMetricsApplication.java # Entry point
│       ├── config/                      # Security (JWT/RBAC), Kafka, Redis configurations
│       ├── controller/                  # Event API, Alerts, Support Bot, Reports API
│       ├── model/                       # JPA Entities and DTOs
│       ├── repository/                  # JPA repositories
│       ├── service/                     # Ingestion, Aggregation, Anomaly Detection, Alerting, Support FAQ, Email Reporting
│       └── consumer/                    # Kafka consumers (Aggregator, Alert Engine, Report Engine)
├── pulse-metrics-frontend/              # React + Vite Dashboard
│   ├── package.json
│   ├── index.html
│   ├── src/
│   │   ├── App.tsx                      # Beautiful dark-mode dashboard UI
│   │   ├── components/                  # KPI cards, Charts, Alerts Feed, FAQ Chatbot
│   │   └── main.tsx
│   └── vite.config.ts
└── simulator/                           # Load Generator
    └── event-simulator.py               # Script generating normal and anomaly load (100-1000 events/sec)
```

### Component Breakdown

---

### 1. Infrastructure (`docker-compose.yml` & SQL)

We will configure the required storage, caching, and ingestion components.

#### [NEW] [docker-compose.yml](file:///Users/akhilesh/Documents/project/Pulse-Metrics/docker-compose.yml)
Contains configurations for:
*   **PostgreSQL**: Configured with a default database `pulse_metrics` and mounting `init.sql`.
*   **Apache Kafka & Zookeeper**: Single-broker Kafka cluster for event streaming.
*   **Redis**: In-memory cache for API endpoints and the FAQ intent-matching database.

#### [NEW] [init.sql](file:///Users/akhilesh/Documents/project/Pulse-Metrics/init.sql)
Defines the schema including:
*   `metrics_raw` table (optional/debug) and `metrics_aggregated` partitioned tables.
*   `alerts` table to log fired threshold violations.
*   `support_tickets` table for escalated customer service requests.

---

### 2. Spring Boot Backend (`pulse-metrics-backend`)

The backend will implement the three core segments described in your resume point.

#### [NEW] [pom.xml](file:///Users/akhilesh/Documents/project/Pulse-Metrics/pulse-metrics-backend/pom.xml)
Includes parent `spring-boot-starter-parent` (version `3.3.4`) and dependencies:
*   `spring-boot-starter-web` & `spring-boot-starter-websocket` (for live UI streaming)
*   `spring-boot-starter-data-jpa` & `postgresql` driver
*   `spring-boot-starter-data-redis`
*   `spring-kafka`
*   `spring-boot-starter-security` (JWT-based token parsing and RBAC filters)

#### [NEW] [Security Config & Context](file:///Users/akhilesh/Documents/project/Pulse-Metrics/pulse-metrics-backend/src/main/java/com/pulse/metrics/config/SecurityConfig.java)
*   Implements a custom JWT authentication filter.
*   Decodes role memberships (`Admin`, `Analyst`, `Viewer`) and Tenant ID from custom claim headers.
*   Propagates Tenant ID through a `ThreadLocal` context to enforce tenant isolation in PostgreSQL queries.

#### [NEW] [Kafka Ingestion & Consumers](file:///Users/akhilesh/Documents/project/Pulse-Metrics/pulse-metrics-backend/src/main/java/com/pulse/metrics/consumer/)
*   **Ingestion Endpoint**: A REST controller `/api/v1/events` accepting fast payloads (tenant ID, event type, timestamp, metric value) and writing them directly to Kafka.
*   **Aggregation Consumer**: Subscribes to the event topic, maintains sliding windows (1m, 5m, 1h, 24h) in-memory or Redis, and writes windowed statistics to `metrics_aggregated` in PostgreSQL.
*   **Anomaly Detection & Alert Consumer**: Calculates Z-scores ($Z = \frac{x - \mu}{\sigma}$) of ingestion rates. If $Z > 2.5$, it publishes to the anomaly topic, where the alert consumer generates in-app notification events (sent via WebSocket/SSE to UI) and logs them to the DB.
*   **Report Consumer**: Listens on a report request topic, generates mock CSV/PDF metrics, and saves them to a static directory.

#### [NEW] [Support Bot Service](file:///Users/akhilesh/Documents/project/Pulse-Metrics/pulse-metrics-backend/src/main/java/com/pulse/metrics/service/SupportBotService.java)
*   Stores an FAQ index in Redis.
*   Performs quick keyword intent matching.
*   If confidence is low, creates a support ticket in PostgreSQL database with `Auto-Escalated` status and a REST response.

---

### 3. Frontend Dashboard (`pulse-metrics-frontend`)

A modern, high-fidelity dark UI built to wow the viewer.

#### [NEW] [Dashboard UI](file:///Users/akhilesh/Documents/project/Pulse-Metrics/pulse-metrics-frontend/src/App.tsx)
*   **Dark Glassmorphic Theme**: Sophisticated slate-900 background with translucent panels (`backdrop-blur`).
*   **KPI Metric Cards**: Real-time throughput (events/sec), system CPU load/latency, and alert count.
*   **Interactive Event Graph**: Real-time line chart (Recharts) displaying live aggregated metrics from a WebSocket/SSE feed.
*   **Alert Feed**: Alerts scrolling in real-time, flashing red on new anomalies.
*   **Tenant Switcher**: Dropdown simulating login as different Tenants to demonstrate multi-tenancy separation.
*   **Support Bot Chat widget**: Let's users chat with the Redis FAQ bot.

---

### 4. Simulator (`simulator/event-simulator.py`)

#### [NEW] [event-simulator.py](file:///Users/akhilesh/Documents/project/Pulse-Metrics/simulator/event-simulator.py)
*   A Python load script using `kafka-python` or standard REST requests.
*   Generates a continuous flow of events mimicking normal operations.
*   Simulates random spikes in metric values or traffic volume every 30 seconds to trigger Z-score anomalies and fire alerts.

---

## Verification Plan

### Automated Tests
*   Run unit/integration tests on backend to verify:
    *   JWT tenant isolation filters.
    *   Z-score anomaly detection math.
    *   Redis FAQ keyword matching.

### Manual Verification
1.  **Boot Up Infrastructure**: Run `docker-compose up -d` in the project root.
2.  **Launch Backend**: Start the Spring Boot application.
3.  **Launch Simulator**: Run the python load script and watch events queue up.
4.  **Launch Frontend**: Run `npm run dev` and open the browser.
5.  **Verify Flow**: Check that the live chart updates, click tenant options to see isolation, chat with the support bot, and watch anomaly alerts pop up when simulated traffic spikes occur.
