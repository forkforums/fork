# Fork

Fork is a minimal federated, ActivityPub-inspired Reddit clone written in Clojure.

Each node can:

- host local forums
- publish posts into those forums
- discover remote forums via WebFinger-style lookup
- poll remote outboxes and aggregate posts into a local feed

This MVP is intentionally small:

- in-memory storage only
- pull-based federation only
- no auth, voting, moderation, or signatures

## Stack

- Clojure
- Ring
- Reitit
- Cheshire
- clj-http

## Project Layout

- `src/fork/core.clj` - server startup and route wiring
- `src/fork/store.clj` - in-memory atoms and query helpers
- `src/fork/actor.clj` - ActivityPub-style forum actor
- `src/fork/outbox.clj` - forum outbox generation
- `src/fork/discovery.clj` - WebFinger response and forum resolution
- `src/fork/federation.clj` - background polling worker
- `src/fork/http/handler/forum.clj` - forum, post, and subscribe handlers
- `src/fork/http/handler/feed.clj` - aggregated feed handler

## Requirements

- Java installed
- Leiningen installed

## Running

Start a single node on the default port:

```bash
lein run
```

Start a node on a custom port:

```bash
FORK_PORT=5001 FORK_BASE_URL=http://localhost:5001 lein run
```

### Environment Variables

- `FORK_PORT` - HTTP port, defaults to `5000`
- `FORK_HOST` - bind host, defaults to `0.0.0.0`
- `FORK_BASE_URL` - base URL used in generated actor and post IDs
- `FORK_SEEDS` - comma-separated seed node URLs for remote forum discovery

## Dev Script

Use the included script to start a local node with sensible defaults:

```bash
./dev.sh
```

Start a node on a specific port:

```bash
./dev.sh 5001
```

Start a second node pointed at the first node for discovery:

```bash
./dev.sh 5002 http://localhost:5001
```

The script sets:

- `FORK_PORT`
- `FORK_BASE_URL`
- `FORK_SEEDS` when a seed is provided

## API

### Local API

- `POST /forum`
- `POST /post/:forum`
- `GET /forum/:forum/posts`
- `POST /subscribe`
- `GET /feed`

### ActivityPub-style

- `GET /actor/:forum`
- `GET /outbox/:forum`

### Discovery

- `GET /.well-known/webfinger?resource=forum:<forum-name>`

## Example Workflow

Create a forum:

```bash
curl -X POST http://localhost:5001/forum \
  -H 'Content-Type: application/json' \
  -d '{"name":"tech"}'
```

Create a post:

```bash
curl -X POST http://localhost:5001/post/tech \
  -H 'Content-Type: application/json' \
  -d '{"content":"hello federation"}'
```

List posts in a forum:

```bash
curl http://localhost:5001/forum/tech/posts
```

Look up a forum via WebFinger:

```bash
curl 'http://localhost:5001/.well-known/webfinger?resource=forum:tech'
```

Inspect the actor:

```bash
curl http://localhost:5001/actor/tech
```

Inspect the outbox:

```bash
curl http://localhost:5001/outbox/tech
```

Subscribe from another node:

```bash
curl -X POST http://localhost:5002/subscribe \
  -H 'Content-Type: application/json' \
  -d '{"forum":"tech","seed":"http://localhost:5001"}'
```

Fetch the aggregated feed:

```bash
curl http://localhost:5002/feed
```

## Local Federation Demo

Terminal 1:

```bash
./dev.sh 5001
```

Terminal 2:

```bash
./dev.sh 5002 http://localhost:5001
```

Then:

1. Create `tech` on node 1
2. Post to `tech` on node 1
3. Subscribe to `tech` on node 2
4. Wait about 10 seconds for the polling loop
5. Fetch `http://localhost:5002/feed`

## Notes

- Data is stored in memory and resets on restart
- Each forum has a single host node in this MVP
- Federation is pull-only; there is no inbox push flow yet
