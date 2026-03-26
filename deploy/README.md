# UrbanLift Deploy Folder

This folder separates runtime/deploy assets from app source code.

## Structure

- `docker/` — local Docker Compose stack (infra + app image placeholders)
- `k8s/` — Kubernetes starter manifests

## Notes

- You can keep developing services with `gradlew bootRun` and use Docker only for infra (`mysql`, `redis`, `kafka`).
- Replace placeholder app image names before running full containerized stack.
- Frontend socket live tracking expects Socket service reachable at `:3002` (or via ingress/proxy with `/__socket` in Vite dev).

## Quick start (infra only)

```bash
cd deploy/docker
docker compose -f docker-compose.local.yml up -d mysql redis zookeeper kafka
```

Then run Spring services locally (`bootRun`) and start frontend (`npm run dev`).
