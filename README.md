# Student Task Planner
Spring Boot 3 / Java 17 task manager with in-memory storage (tasks reset on restart).

Run: `mvn clean test` then `mvn spring-boot:run`, open http://localhost:8080. Health: `/api/health`.
Docker: `docker build -t student-task-planner:local .` then `docker run --rm -p 8080:8080 student-task-planner:local`.

## Jenkins setup
- Global tools: JDK named `JDK17`, Maven named `Maven3`. Agent needs Docker, kubectl, Git.
- Credentials: `acr-credentials` (Username with password, ACR admin user), `aks-kubeconfig` (Secret file).
- Edit `REPLACE_ACR_NAME` in `Jenkinsfile` and `k8s/deployment.yaml`.
- Pipeline job from SCM (your GitHub repo, branch main) with the GitHub webhook / "GitHub hook trigger".
Azure steps: see `docs/azure-commands.md`.
