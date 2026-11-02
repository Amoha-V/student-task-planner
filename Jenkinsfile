
pipeline {
    agent any

    environment {
        // Google Cloud configuration
        GCP_PROJECT = 'axiomatic-set-443307-k3'
        GCP_REGION = 'asia-south1'
        AR_REPOSITORY = 'student-planner-repo'

        // Docker image configuration
        IMAGE = 'asia-south1-docker.pkg.dev/axiomatic-set-443307-k3/student-planner-repo/student-task-planner'
        TAG = "${env.BUILD_NUMBER}"

        // GKE configuration
        GKE_CLUSTER = 'student-planner-cluster'
        GKE_ZONE = 'asia-south2-a'
        DEPLOY_TO_GKE = 'true'

        // Maven configuration
        MAVEN = 'C:\\Apache\\maven\\bin\\mvn.cmd'

        // WSL configuration
        WSL_DISTRO = 'Ubuntu'
    }

    stages {

        // 1. Checkout source code
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        // 2. Checkov security scanning
        stage('Checkov Security Scan') {
            steps {
                bat '''
                    docker run --rm ^
                    -v "%WORKSPACE%:/src" ^
                    bridgecrew/checkov:latest ^
                    --directory /src ^
                    --framework kubernetes,dockerfile ^
                    --soft-fail
                '''
            }
        }

        // 3. Validate Ansible playbook
        stage('Validate Ansible Playbook') {
            steps {
                bat '''
                    wsl.exe -d %WSL_DISTRO% -- bash -lc "WORKSPACE_WSL=$(wslpath -u '%WORKSPACE%') && cd \\"$WORKSPACE_WSL\\" && ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook ansible/deploy.yml --syntax-check"
                '''
            }
        }

        // 4. Build and test Java application
        stage('Build and Test') {
            steps {
                bat '"%MAVEN%" -B clean package'
            }

            post {
                always {
                    junit allowEmptyResults: true,
                          testResults: 'target/surefire-reports/*.xml'
                }
            }
        }

        // 5. Build Docker image
        stage('Build Docker Image') {
            steps {
                bat '''
                    docker build -t %IMAGE%:%TAG% -t %IMAGE%:latest .
                '''
            }
        }

        // 6. Authenticate with Google Cloud
        stage('Authenticate with Google Cloud') {
            steps {
                withCredentials([
                    file(
                        credentialsId: 'gcp-service-account-json',
                        variable: 'GCP_KEY'
                    )
                ]) {
                    bat '''
                        gcloud auth activate-service-account --key-file="%GCP_KEY%"
                        gcloud config set project %GCP_PROJECT%
                        gcloud auth configure-docker asia-south1-docker.pkg.dev --quiet
                    '''
                }
            }
        }

        // 7. Push Docker image to Artifact Registry
        stage('Push to Artifact Registry') {
            steps {
                bat 'docker push %IMAGE%:%TAG%'
                bat 'docker push %IMAGE%:latest'
            }
        }

        // 8. Deploy to GKE using Ansible
        stage('Deploy to GKE using Ansible') {
            when {
                expression {
                    return env.DEPLOY_TO_GKE == 'true'
                }
            }

            steps {
                withCredentials([
                    file(
                        credentialsId: 'gcp-service-account-json',
                        variable: 'GCP_KEY'
                    )
                ]) {
                    powershell '''
                        $ErrorActionPreference = "Stop"

                        # Convert the Jenkins credential path to a WSL path.
                        $keyPath = (
                            wsl.exe -d Ubuntu -- wslpath -u "$env:GCP_KEY"
                        ).Trim()

                        if ($LASTEXITCODE -ne 0 -or !$keyPath) {
                            throw "Could not convert the GCP credential path."
                        }

                        # Store the converted path in the Jenkins workspace.
                        $pathFile = Join-Path $env:WORKSPACE "gcp-key-path.txt"

                        Set-Content -Path $pathFile -Value $keyPath -NoNewline

                        # Pass required Jenkins environment variables into WSL.
                        $oldWslEnv = $env:WSLENV

                        $env:WSLENV = "WORKSPACE/p:BUILD_NUMBER:GCP_PROJECT:GKE_CLUSTER:GKE_ZONE:IMAGE:TAG"

                        try {
                            wsl.exe -d Ubuntu -- bash -lc '
                                set -euo pipefail

                                echo "Starting GKE deployment..."

                                # WORKSPACE is translated into a WSL path.
                                WORKSPACE_WSL="$WORKSPACE"

                                # Read the WSL-compatible service-account path.
                                KEY_WSL=$(cat "$WORKSPACE_WSL/gcp-key-path.txt")

                                if [[ ! -f "$KEY_WSL" ]]; then
                                    echo "ERROR: GCP credential file not found."
                                    exit 1
                                fi

                                # Isolate Google Cloud credentials for this build.
                                export CLOUDSDK_CONFIG="/tmp/gcloud-jenkins-$BUILD_NUMBER"
                                export KUBECONFIG="$CLOUDSDK_CONFIG/kubeconfig"

                                mkdir -p "$CLOUDSDK_CONFIG"

                                # Remove temporary Google Cloud configuration.
                                cleanup() {
                                    rm -rf "$CLOUDSDK_CONFIG"
                                }

                                trap cleanup EXIT

                                cd "$WORKSPACE_WSL"

                                # Authenticate using the Jenkins service account.
                                gcloud auth activate-service-account \
                                    --key-file="$KEY_WSL" \
                                    --project="$GCP_PROJECT"

                                # Connect to the GKE cluster.
                                gcloud container clusters get-credentials \
                                    "$GKE_CLUSTER" \
                                    --zone "$GKE_ZONE" \
                                    --project "$GCP_PROJECT"

                                # Deploy the new Docker image using Ansible.
                                ANSIBLE_CONFIG=ansible/ansible.cfg \
                                ansible-playbook ansible/deploy.yml \
                                    -e "image_tag=${IMAGE}:${TAG}"

                                echo "GKE deployment completed successfully."
                            '

                            if ($LASTEXITCODE -ne 0) {
                                throw "GKE deployment failed."
                            }
                        }
                        finally {
                            # Remove temporary path file.
                            Remove-Item $pathFile -Force -ErrorAction SilentlyContinue

                            # Restore previous WSL environment configuration.
                            $env:WSLENV = $oldWslEnv
                        }
                    '''
                }
            }
        }

        // 9. Verify Docker image
        stage('Verify Docker Image') {
            steps {
                bat 'docker images %IMAGE%'
            }
        }
    }

    post {
        success {
            echo 'Student Task Planner CI/CD Pipeline completed successfully!'
        }

        failure {
            echo 'Student Task Planner CI/CD Pipeline failed.'
        }

        always {
            echo 'Jenkins pipeline execution finished.'
        }
    }
}
