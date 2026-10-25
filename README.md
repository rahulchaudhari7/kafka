# Spring Boot Continuous Deployment with Canary Releases

This project is a small Spring Boot storefront and product catalog. Its GitHub
Actions pipeline runs the test suite, builds a Docker image, publishes an
immutable commit-tagged image to GitHub Container Registry (GHCR), and deploys
it to Kubernetes through Argo Rollouts.

The rollout sends traffic to the new version in stages: **10% → 30% → 60% →
100%**, pausing for one minute at each intermediate step. NGINX Ingress performs
the traffic split. Startup and readiness probes gate rollout progress; if the
new pods do not become ready, the rollout does not advance. A rollout that
exceeds its 10-minute progress deadline is automatically aborted, keeping the
last stable ReplicaSet available. The configured
pauses are timed observation windows, not metric-based analysis.

## Run locally

Requirements: Java 17 and Maven 3.9 or newer.

```sh
mvn --batch-mode verify
mvn spring-boot:run
```

Open `http://localhost:8080` for the storefront. The API endpoints are
`GET /api/products` and `GET /api/products/{id}`. Spring Boot Actuator provides
`/actuator/health` and `/actuator/health/readiness`.

Build and run the container locally:

```sh
docker build -t storefront:local .
docker run --rm -p 8080:8080 storefront:local
```

The frontend is served from the existing `public/` assets; the Maven build
packages them into the Spring Boot application.

## One-time Kubernetes prerequisites

The target cluster needs Kubernetes, an NGINX Ingress controller, and the Argo
Rollouts controller/CRDs. For a demo cluster, install the controllers before
the first application deployment:

```sh
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.12.1/deploy/static/provider/cloud/deploy.yaml
kubectl apply -f https://github.com/argoproj/argo-rollouts/releases/download/v1.8.3/install.yaml
```

The Rollout is defined in `k8s/storefront.yaml`. Update
`storefront.example.com` there to the host/DNS name routed to your ingress
controller. Configure TLS before using the example for a public production
service.

## Configure GitHub Actions

Push the project to GitHub and add these repository secrets (or secrets to the
`production` environment):

| Secret | Purpose |
| --- | --- |
| `KUBECONFIG_BASE64` | Base64-encoded kubeconfig for the target cluster. Give this identity only the namespace-scoped permissions needed to manage the storefront rollout, services, ingress, and image pull secret. |
| `GHCR_USERNAME` | GitHub username that can read the published package. |
| `GHCR_READ_TOKEN` | GitHub personal access token with `read:packages` permission, used by Kubernetes to pull the GHCR image. |

Encode kubeconfig as a single line on Linux/macOS with
`base64 -w 0 < kubeconfig`; on PowerShell use
`[Convert]::ToBase64String([IO.File]::ReadAllBytes("kubeconfig"))`.

The `production` GitHub Actions environment can optionally require approval
before deployment. The workflow publishes and deploys only pushes to `main`
(and manual runs targeting `main`); pull requests run verification only. The
GHCR package must be readable by the account used for `GHCR_READ_TOKEN`.

The deployment job creates/updates `ghcr-pull-secret`, applies the Kubernetes
manifests with the commit-specific image, then waits for Argo Rollouts to
complete the canary. Check the rollout with:

```sh
kubectl argo rollouts get rollout storefront --namespace storefront --watch
```

## Tests

`mvn verify` runs the catalog unit tests, Spring Boot HTTP integration tests,
frontend static-asset checks, health/readiness checks, and the product response
contract test using `contracts/storefront-products.v1.json`.
