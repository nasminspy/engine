# Order Matching Engine - HELP

## 1. How to Run the Application
- Build: `./mvnw clean install`
- Run: `./mvnw spring-boot:run`
- Default port: `8080` (configurable via `application.properties`)

## 2. API Endpoints
| Method | Endpoint               | Description                 |
|--------|------------------------|-----------------------------|
| POST   | `/orders`              | Submit a new order          |
| GET    | `/orders/{id}`         | Retrieve order by ID        |
| GET    | `/orders/symbol/{symbol}` | List orders by symbol with pagination |

## 3. How to Run Tests and Coverage
- Run tests: `./mvnw test`
- Generate coverage report: `./mvnw jacoco:report`
- Report path: `target/site/jacoco/index.html`

## 4. Order Matching Logic
- Orders stored in separate priority queues per symbol for BUY and SELL.
- Matching uses price-time priority:
    - BUY queue sorted by descending price, then earliest timestamp.
    - SELL queue sorted by ascending price, then earliest timestamp.
- Matching process (locked per symbol):
    - While top BUY price ≥ top SELL price:
        - Match minimum quantity between top orders.
        - Decrement quantities accordingly.
        - Remove fully matched orders from queues.
- Handles fully matched, partially matched, and unmatched orders fairly and efficiently.

## 5. Handling Increased Order Volumes
- Uses thread pool and asynchronous processing.
- Blocking queue buffers incoming orders.
- Symbol-level locks maximize concurrency with data safety.
- Retry mechanism with backoff for fault tolerance.
- Dead-letter queue handles failed orders for later review.

## 6. Coding Best Practices
- Thread-safe data structures and locks for concurrency.
- Clear separation: OrderService manages processing, OrderManager manages order storage & matching.
- Logging and error handling with retries.
- Pagination support for large data queries.
- Use of Lombok for clean model code.

## 7. Potential Future Enhancements
- Persistent storage for durability and recovery.
- Real-time trade updates via WebSocket.
- Support complex order types (limit, stop).
- Audit logs for partial fills.
- Alerting and manual retry for dead-letter queue.
- Monitoring and metrics dashboards.

---

*This document provides an overview of the Order Matching Engine usage, architecture, and design considerations.*
