# D2DB — Distributed Database Management System

A Java-based database engine built from scratch — featuring a custom SQL parser, file storage engine, cross-VM replication, transaction management, and a structured logging system.

> Built to understand how databases work at the engine level: from tokenizing raw SQL to persisting data in a custom file format and syncing it across machines.

---

## What It Does

D2DB is a functional database system that accepts SQL via a CLI, parses and executes it, persists data to disk in a custom format (`.d2db`), and replicates changes across virtual machines over TCP sockets.

**Supported Operations:**
- `CREATE TABLE` with column types, `PRIMARY KEY`, `UNIQUE`, `FOREIGN KEY`
- `INSERT INTO` with value validation
- `SELECT * FROM` with `WHERE` filtering
- ERD generation from live schema
- SQL dump export (streamed to avoid OOM)
- User registration and login with SHA-256 hashed passwords

---

## Architecture Overview

```
CLI Input (AppCLI)
    │
    ▼
Tokenizer  ──→  Token List  ──→  SQLParser  ──→  QueryExecutor (interface)
                                                       │
                              ┌────────────────────────┼──────────────────────┐
                              ▼                        ▼                      ▼
                   CreateTableExecutor       InsertExecutor          SelectExecutor
                              │                        │
                              ▼                        ▼
                      LocalMetadataManager      TransactionManager
                              │                   (in-memory workspace)
                              ▼                        │
                      CustomFileWriter  ◀──────────────┘
                              │
                              ▼
                    .d2db File (disk)  ──→  VMSyncClient  ──→  VMSyncServer (replica)
```

---

## Data Structures — Chosen With Intent

| Structure | Where Used | Why |
|---|---|---|
| `LinkedList<List<String>>` | `Table.java` — stores rows | O(1) appends for sequential inserts; database writes are append-heavy |
| `ArrayList<ColumnMetadata>` | `Table.java` — schema | Schema is read far more than modified; O(1) index access |
| `ConcurrentHashMap<K,V>` | `LocalMetadataManager`, `AuthenticationManager` | Multiple threads (query thread, sync server thread) read/write metadata concurrently — avoids full-table locks of `synchronizedMap` |
| `HashMap<String, Table>` | `TransactionManager` | In-memory workspace per transaction; single-threaded access within a transaction context |
| `Trie` + `TrieNode (HashMap<Char, TrieNode>)` | `Table.java` — index | Custom implementation; indexes primary key values for prefix-based lookups. Each node maps a character to its child — O(L) insert/search where L = key length |
| `ThreadLocal<String>` | `ExecutionContext.java` | Isolates `currentDatabase` and `userId` per thread — eliminates passing context through every method call, mirrors how JDBC stores connection state |
| `ExecutorService` (fixed thread pool) | `VMSyncServer.java` | Handles multiple incoming replica sync connections concurrently without spawning unbounded threads |
| `ReentrantLock` (fair mode) | `TransactionManager.java` | Ensures transactions are granted in arrival order; prevents starvation in multi-user scenarios |

---

## Design Patterns — Applied to Real Problems

### 1. Command Pattern — `QueryExecutor` Interface

**Problem:** `CREATE`, `INSERT`, `SELECT` all need to be executable objects that can be queued, replicated, and passed around — not just called inline.

```java
// QueryExecutor.java
public interface QueryExecutor {
    void execute(boolean isReplicaSync) throws Exception;
}

// SQLParser.java produces the right command at parse time:
case "CREATE" -> return parseCreate(); // returns CreateTableExecutor
case "INSERT" -> return parseInsert(); // returns InsertExecutor
case "SELECT" -> return parseSelect(); // returns SelectExecutor
```

The `isReplicaSync` flag on `execute()` is what prevents infinite replication loops — a replica receiving a sync command passes `true` so it does not broadcast back.

---

### 2. Singleton Pattern — Managers and Loggers

**Problem:** `LocalMetadataManager`, `TransactionManager`, `AuthenticationManager`, `ConfigManager`, and all three loggers must be shared across the system with exactly one instance.

```java
// LocalMetadataManager.java — thread-safe lazy initialization
public static synchronized LocalMetadataManager getInstance() {
    if (instance == null) {
        instance = new LocalMetadataManager();
    }
    return instance;
}
```

All Singleton instances use `synchronized` on `getInstance()`. The `volatile` keyword is the next improvement to make (double-checked locking).

---

### 3. Template Method Pattern — Logging System

**Problem:** All three loggers (`EventLogger`, `GeneralLogger`, `QueryLogger`) share the same file-append mechanics but write different JSON payloads.

```java
// LogManager.java — abstract base defines the HOW
protected synchronized void appendLog(String fileName, String jsonPayLoad) { ... }

// EventLogger.java — subclass defines the WHAT
public void logEvent(String eventType, String details, String vmID) {
    String json = String.format("{\"timestamp\":\"%s\", \"eventType\":\"%s\"...}", ...);
    appendLog("EventLog.json", json); // delegates mechanics to parent
}
```

Adding a new logger type requires zero changes to the base class — just extend and call `appendLog`.

---

### 4. Iterator Pattern — Memory-Safe File Reading

**Problem:** Loading an entire large table into memory to export or display it causes Out-of-Memory errors.

```java
// CustomFileReader.java — returns a lazy Iterator, not a full List
public Iterator<List<String>> streamTableRow(String tableName) throws Exception {
    BufferedReader reader = new BufferedReader(new FileReader(file));
    return new Iterator<List<String>>() {
        String nextLine = fetchNext();
        // reads one line at a time, keeps only one row in memory
        @Override public List<String> next() { ... }
    };
}

// SQLDumpGenerator.java — consumes it row by row
Iterator<List<String>> rowStream = reader.streamTableRow(tableName);
while (rowStream.hasNext()) {
    writer.write(buildInsertStatement(rowStream.next()));
}
```

---

### 5. Context Object Pattern — `ExecutionContext`

**Problem:** Every executor needs to know the current database and user, but threading them through every method signature is messy and fragile.

```java
// ExecutionContext.java
private static final ThreadLocal<String> currentDbName = new ThreadLocal<>();
private static final ThreadLocal<String> currentUserId = new ThreadLocal<>();
```

`ThreadLocal` makes the context invisible to method signatures while remaining thread-safe. `ExecutionContext.clear()` is called in the `finally` block of every query path to prevent context leakage between requests.

---

## Key Engineering Decisions

**Custom file format (`.d2db` with `|#|` delimiter)**
Rather than JSON or CSV, a custom delimiter was chosen to avoid conflicts with commas or quotes that could appear in user data. `CustomFileWriter.writeTable()` is `synchronized` to prevent concurrent write corruption.

**SHA-256 password hashing in `AuthenticationManager`**
Passwords are never stored in plaintext. `MessageDigest.getInstance("SHA-256")` hashes them before writing to `User_Profile.txt`. The in-memory `ConcurrentHashMap` cache avoids re-reading the file on every login.

**Watchdog thread in `TransactionManager`**
A daemon thread starts on initialization to detect and roll back timed-out transactions (60s timeout). Using `setDaemon(true)` ensures it does not prevent JVM shutdown.

**VM replication via raw TCP sockets**
`VMSyncServer` runs in its own daemon thread, accepts connections via a fixed thread pool, parses the incoming SQL payload, and re-executes it locally with `isReplicaSync=true`. `VMSyncClient.broadcastCommit()` wraps the payload with database and user context before sending.

---

## Project Structure

```
com.d2db/
├── AppCLI.java                  # CLI entry point and menu
├── auth/
│   └── AuthenticationManager    # SHA-256 hashing, user cache, file-backed persistence
├── config/
│   └── ConfigManager            # .env file reader (Singleton)
├── engine/
│   ├── Tokenizer                # Regex-based SQL lexer
│   ├── Token / TokenType        # Token value objects and enum
│   ├── ExecutionContext         # ThreadLocal per-request context
│   ├── VMID                     # Resolves machine hostname
│   ├── parser/
│   │   ├── SQLParser            # Recursive descent parser → produces QueryExecutors
│   │   └── QueryExecutor        # Command interface
│   └── executor/
│       ├── CreateTableExecutor
│       ├── InsertExecutor
│       ├── SelectExecutor
│       └── DeleteExecutor       
├── model/
│   ├── Table                    # In-memory table: LinkedList rows + Trie index
│   ├── ColumnMetadata           # Schema: name, type, PK/FK/UNIQUE flags
│   └── Trie / TrieNode          # Custom Trie for primary key indexing
├── storage/
│   ├── CustomFileWriter         # Writes .d2db files (synchronized)
│   ├── CustomFileReader         # Reads .d2db files; supports lazy Iterator streaming
│   ├── LocalMetadataManager     # Node-local table registry (Singleton + ConcurrentHashMap)
│   └── GlobalMetadataManager    # Cross-VM schema registry (Singleton + ConcurrentHashMap)
├── transaction/
│   └── TransactionManager       # ReentrantLock, in-memory workspace, watchdog thread
├── network/
│   ├── VMSyncServer             # TCP server with thread pool for cross-VM replication
│   └── VMSyncClient             # Broadcasts SQL commits to replica VMs
├── logging/
│   ├── LogManager               # Abstract base (Template Method)
│   ├── EventLogger              # Logs system events as JSON
│   ├── GeneralLogger            # Logs execution time and DB state as JSON
│   └── QueryLogger              # Logs per-query audit trail as JSON
└── tools/
    ├── erd/ERDGenerator         # Generates text ERD from live schema with FK cardinality
    └── export/SQLDumpGenerator  # Exports full SQL dump using streamed reads
```

---

## Running the Project

**Prerequisites:** Java 17+ (no build tools required — pure Java)

```bash
git clone https://github.com/kov08/ddms.git
cd ddms/src/main/java
```

**Compile all files at once:**
```bash
javac -d out $(find . -name "*.java")
```

**Run:**
```bash
java -cp out com.d2db.AppCLI
```

Or simply open the project in **IntelliJ IDEA** or **VS Code** and run `AppCLI.java` directly.

**Optional:** Create a `.env` file in the project root to configure the sync port:
```
VMSync_IP
SYNC_PORT=9090
```
