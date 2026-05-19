Fix the red-dragon deploy pipeline so CI-driven deploys actually roll out
new backend builds. Two bugs landed together in `/opt/gc/.env` and
`/opt/gc/deploy.sh` and silently broke deploys after #828: `GC_IMAGE` was
digest-pinned to a stale SHA so `docker compose pull` never picked up new
CI builds, and `deploy.sh`'s post-deploy health check `curl`ed
`localhost:8000` even though the backend now binds to the tailnet IP only.
The deploy job was failing on every push to `main`, the image was getting
to red-dragon only by manual `docker compose pull` from a logged-in user,
and CI was unable to report deploy success. Switch `GC_IMAGE` to the
floating `...:main` tag, run the health check inside the backend container
via `docker compose exec ... wget` (the JRE Alpine base image ships `wget`,
not `curl`), and commit the canonical `deploy.sh` + `.env.example` +
README under `deploy/docker/` so the two artifacts that previously lived
only on red-dragon now have a tracked source of truth.
