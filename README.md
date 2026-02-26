<h1 align="center">Kairos Engine</h1>
<p align="center">
  Kotlin-native distributed workflow orchestration engine.<br>
  Kairos coordinates multi-step,<br>
  multi-service business processes with built-in fault tolerance, automatic retries,<br>
  and Saga-based compensation when things go wrong.
</p>
<p align="center">
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/license-MIT-f9e2af?style=flat&labelColor=1e1e2e" alt="License: MIT">
  </a>
</p>
<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-b4befe?style=flat&labelColor=1e1e2e&logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Spring%20Boot-a6e3a1?style=flat&labelColor=1e1e2e&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Spring%20WebFlux-94e2d5?style=flat&labelColor=1e1e2e&logo=spring&logoColor=white" alt="Spring WebFlux">
  <img src="https://img.shields.io/badge/PostgreSQL-89b4fa?style=flat&labelColor=1e1e2e&logo=postgresql&logoColor=white" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-f38ba8?style=flat&labelColor=1e1e2e&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Apache%20Kafka-a6adc8?style=flat&labelColor=1e1e2e&logo=apachekafka&logoColor=white" alt="Apache Kafka">
  <img src="https://img.shields.io/badge/gRPC-cba6f7?style=flat&labelColor=1e1e2e&logo=grpc&logoColor=white" alt="gRPC">
  <img src="https://img.shields.io/badge/OpenTelemetry-fab387?style=flat&labelColor=1e1e2e&logo=opentelemetry&logoColor=white" alt="OpenTelemetry">
</p>

---

## Why Kairos?

In distributed systems, a single user action can touch dozens of services. When Step 4 out of 7 fails, the first three steps have already made changes — payments charged, inventory reserved, emails sent. Someone needs to undo all of that, in the right order, reliably.

Existing solutions (Temporal, Camunda, Conductor) solve this but require dedicated infrastructure and steep learning curves. Kairos solves the same problem as an embeddable, Kotlin-native engine that integrates directly into your existing Spring Boot application.

---

## Planned Features

🧩 **Kotlin DSL Workflow Definitions** Define workflows as readable, type-safe Kotlin code. Steps, dependencies, parallel execution groups, compensation actions, and retry policies are all declared in a single, cohesive DSL using lambda with receiver, sealed classes, and reified generics.

📊 **DAG-Based Execution** Steps and their dependencies form a Directed Acyclic Graph. Kairos resolves execution order via topological sort and automatically runs independent steps in parallel using Kotlin Coroutines with structured concurrency.

🔄 **Smart Retry Strategies** Each step gets its own retry policy: fixed delay, exponential backoff, or exponential backoff with jitter. The engine distinguishes retryable errors (network timeouts, 503s) from permanent failures (validation errors, 400s) and only retries when it makes sense.

⏪ **Saga Pattern Compensation** Every step can define a compensation action (its "undo"). When a step fails permanently, Kairos executes all previous compensations in reverse chronological order. If compensation itself fails, the workflow is flagged for manual intervention.

🛡️ **Circuit Breaker** Tracks downstream service health with CLOSED → OPEN → HALF_OPEN states. Prevents cascading failures by stopping requests to services that are clearly down, giving them time to recover.

📦 **Event Sourced State** Every state change is persisted as an immutable event. This provides a full audit trail, crash recovery (replay from last event), and the ability to reconstruct workflow state at any point in time. PostgreSQL as primary store with optional Redis caching layer.

📡 **Observability** Micrometer metrics (workflow/step durations, error rates, retry counts), OpenTelemetry distributed tracing (each workflow is a trace, each step is a span), and structured JSON logging with workflow/step correlation IDs.

⚡ **Flexible Triggers** Start workflows via REST API, Kafka events, programmatic calls, or cron-like schedules. Every step completion also publishes events to Kafka for downstream consumers.

🔧 **Operational API** Start, pause, resume, cancel, and retry-from-step via REST and gRPC. Exposes workflow status, history, and inconsistency queries for external consumers and monitoring tools.

---

## How It Works

```
Trigger (REST / Kafka / Code / Schedule)
                   │
                   ▼
┌─────────────────────────────────────────────┐
│            Workflow Registry                │
│   (validated, immutable definitions)        │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│           Execution Engine                  │
│                                             │
│  1. Resolve DAG → compute execution waves   │
│  2. Dispatch steps as coroutines            │
│  3. On success → write to context → next    │
│  4. On failure → retry with backoff/jitter  │
│  5. On permanent failure → compensate       │
│     in reverse order (Saga)                 │
│  6. Persist every transition as event       │
└──────────────────┬──────────────────────────┘
                   ▼
┌─────────────────────────────────────────────┐
│          Observability Layer                │
│  Metrics · Tracing · Structured Logging     │
└─────────────────────────────────────────────┘
```

**Workflow states:** `PENDING` → `RUNNING` → `COMPLETED` on success. On failure: `RUNNING` → `COMPENSATING` → `COMPENSATED` if rollback succeeds, or `FAILED_WITH_INCONSISTENCY` if rollback also fails (requires manual intervention).

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Concurrency | Kotlin Coroutines + Structured Concurrency |
| Framework | Spring Boot + Spring WebFlux |
| Messaging | Apache Kafka |
| State Store | PostgreSQL (primary) + Redis (cache, optional) |
| API | Spring WebFlux REST + gRPC |
| Observability | Micrometer + OpenTelemetry + kotlin-logging |
| Testing | JUnit 5 + Kotest + Testcontainers + MockK |

---