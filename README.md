<h1 align="center">Kairos Engine</h1>
<p align="center">
  Kotlin-native workflow orchestration engine.
</p>

---

In distributed systems, a single user action can touch dozens of services. When Step 4 out of 7 fails, the first three steps have already made changes - payments charged, inventory reserved, emails sent. Someone needs to undo all of that, in the right order, reliably.

Existing solutions (Temporal, Camunda, Conductor) solve this but require dedicated infrastructure and steep learning curves. Kairos solves the same problem as an embeddable, Kotlin-native engine that integrates directly into your existing Spring Boot application.

---

## Quick Example

An e-commerce order workflow payment, inventory, shipping, notification with automatic rollback on failure:

```kotlin
val orderWorkflow = workflow("order-processing") {

    step("validate-order") {
        action { ctx ->
            val order = ctx.input<OrderRequest>()
            orderValidator.validate(order)
        }
        retryPolicy {
            strategy = RetryStrategy.NONE
        }
    }

    step("process-payment") {
        dependsOn("validate-order")
        action { ctx ->
            val order = ctx.input<OrderRequest>()
            paymentService.charge(order.paymentMethod, order.totalAmount)
        }
        compensation { ctx ->
            val paymentId = ctx.stepOutput<PaymentResult>("process-payment").transactionId
            paymentService.refund(paymentId)
        }
        retryPolicy {
            strategy = RetryStrategy.EXPONENTIAL_BACKOFF
            maxRetries = 3
            baseDelay = 500.milliseconds
            jitter = true
        }
    }

    step("reserve-inventory") {
        dependsOn("validate-order")
        action { ctx ->
            val order = ctx.input<OrderRequest>()
            inventoryService.reserve(order.items)
        }
        compensation { ctx ->
            val reservation = ctx.stepOutput<ReservationResult>("reserve-inventory")
            inventoryService.release(reservation.reservationId)
        }
        retryPolicy {
            strategy = RetryStrategy.FIXED_DELAY
            maxRetries = 3
            baseDelay = 1.seconds
        }
    }

    step("arrange-shipping") {
        dependsOn("process-payment", "reserve-inventory")
        action { ctx ->
            val order = ctx.input<OrderRequest>()
            val reservation = ctx.stepOutput<ReservationResult>("reserve-inventory")
            shippingService.createShipment(order.shippingAddress, reservation.warehouseId)
        }
        compensation { ctx ->
            val shipment = ctx.stepOutput<ShipmentResult>("arrange-shipping")
            shippingService.cancel(shipment.shipmentId)
        }
    }

    step("send-confirmation") {
        dependsOn("arrange-shipping")
        action { ctx ->
            val order = ctx.input<OrderRequest>()
            val shipment = ctx.stepOutput<ShipmentResult>("arrange-shipping")
            notificationService.sendOrderConfirmation(order.customerEmail, shipment.trackingNumber)
        }
        // no compensation - an extra email is harmless
    }
}
```

Kairos resolves this into a DAG and executes independent steps in parallel:

```
                validate-order
                 /           \
    process-payment    reserve-inventory
                 \           /
              arrange-shipping
                     |
             send-confirmation
```

`process-payment` and `reserve-inventory` run concurrently. If `arrange-shipping` fails permanently, Kairos automatically compensates in reverse order: release inventory → refund payment.

---

## Core Features

🧩 **Kotlin DSL Workflow Definitions** - Define workflows as readable, type-safe Kotlin code. Steps, dependencies, parallel execution groups, compensation actions, and retry policies are all declared in a single, cohesive DSL using lambda with receiver, sealed classes, and reified generics.

📊 **DAG-Based Parallel Execution** - Steps and their dependencies form a Directed Acyclic Graph. Kairos resolves execution order via topological sort and automatically runs independent steps in parallel using Kotlin Coroutines with structured concurrency.

🔄 **Smart Retry Strategies** - Each step gets its own retry policy: fixed delay, exponential backoff, or exponential backoff with jitter. The engine distinguishes retryable errors (network timeouts, 503s) from permanent failures (validation errors, 400s) and only retries when it makes sense.

⏪ **Saga Pattern Compensation** - Every step can define a compensation action (its "undo"). When a step fails permanently, Kairos executes all previous compensations in reverse chronological order. If compensation itself fails, the workflow is flagged for manual intervention.

📦 **Event Sourced State** - Every state change is persisted as an immutable event in PostgreSQL. This provides a full audit trail, crash recovery (replay from last event), and the ability to reconstruct workflow state at any point in time.

---

## How It Works

```
Trigger (REST / Code)
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
| State Store | PostgreSQL |
| API | Spring WebFlux REST |
| Observability | Micrometer + kotlin-logging |
| Testing | JUnit 5 + Kotest + Testcontainers + MockK |

---

## Roadmap

Features planned for future releases, roughly in priority order:

| Feature | Description |
|---|---|
| 🛡️ Circuit Breaker | CLOSED → OPEN → HALF_OPEN state tracking to prevent cascading failures to unhealthy downstream services. |
| 📡 Kafka Integration | Start workflows from Kafka events and publish step completion events for downstream consumers. |
| ⚡ Cron Scheduling | Trigger workflows on cron-like schedules. |
| 🔧 gRPC API | Full operational API (start, pause, resume, cancel, retry-from-step) over gRPC alongside REST. |
| 🗄️ Redis Cache Layer | Optional Redis caching for workflow state to reduce PostgreSQL read load. |
| 📡 OpenTelemetry Tracing | Distributed tracing where each workflow is a trace and each step is a span. |

---
