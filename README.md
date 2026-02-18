# keyValue

A multithreaded key-value server built from scratch in Java. Clients connect over TCP, send plaintext commands, and hit a sharded in-memory store protected by per-shard `ReadWriteLock`s. The server spawns a virtual thread per connection.

## Why Sharding

A single global lock serializes all operations — 100 threads contending on one lock spend most of their time waiting. `ConcurrentHashMap` solves this with internal striping, but it's a black box: you don't control the concurrency level, can't tune lock granularity, and can't separate read/write contention. Sharding with explicit `ReentrantReadWriteLock` per shard gives you that control. Reads within a shard don't block each other, writes only block their own shard, and the shard count is a tunable knob. The benchmark results below show the difference clearly.

## Architecture

```
Client (TCP) --> Server (port 2020)
                   |
                   v
              ServerThread (virtual thread per connection)
                   |
                   v
              DataStore.handler() — parses command string
                   |
                   v
              key.hashCode() % NUM_SHARDS --> shard[i]
                   |
                   v
              ReadWriteLock[i] --> HashMap[i]
```

Incoming text is split into `operation key [value]`. The key's hash determines which shard (and therefore which lock) is used.

## Commands

| Command | Format | Response |
|---------|--------|----------|
| SET | `SET key value` | `OK` |
| GET | `GET key` | value or `NOT FOUND` |
| DEL | `DEL key` | `Deleted!` or `Failed To Delete` |
| EXIT | `EXIT` | `Goodbye!` (closes connection) |

## Benchmarks

Store benchmark — calls `DataStore.handler()` directly, no TCP overhead. 100 virtual threads, 60,000 cycles each (SET + GET + DEL = 180,000 ops/thread, 18M total).

| Shards | Time | Throughput | Contention |
|--------|------|------------|------------|
| 10 | 3,021 ms | **~5.96M ops/sec** | Low — threads spread across 10 locks |
| 1 | 11,992 ms | **~1.50M ops/sec** | High — all threads fight over one lock |

10 shards is **~4x faster**. The workload is identical — the only variable is lock contention. With 1 shard, 100 threads serialize on a single `ReadWriteLock`. With 10, threads are distributed across shards, so most operations proceed in parallel.

Zero failures across all 18M operations in both configurations.

## What I'd Do at Scale

- **MVCC** — instead of locking readers out during writes, maintain versioned entries. Readers see a consistent snapshot without blocking writers. This is how most serious databases (Postgres, SQLite WAL) handle concurrency.
