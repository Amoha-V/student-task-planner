
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
        GKE_ZONE = 'asia-south1-a'
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

        // 8. Deploy using Ansible
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
                    bat '''
                        wsl.exe -d %WSL_DISTRO% -- bash -lc "set -euo pipefail; WORKSPACE_WSL=$(wslpath -u '%WORKSPACE%'); KEY_WSL=$(wslpath -u '%GCP_KEY%'); export CLOUDSDK_CONFIG=/tmp/gcloud-jenkins-%BUILD_NUMBER%; export KUBECONFIG=$CLOUDSDK_CONFIG/kubeconfig; mkdir -p $CLOUDSDK_CONFIG; trap 'rm -rf $CLOUDSDK_CONFIG' EXIT; cd \\"$WORKSPACE_WSL\\"; gcloud auth activate-service-account --key-file=\\"$KEY_WSL\\" --project=%GCP_PROJECT%; gcloud container clusters get-credentials %GKE_CLUSTER% --zone %GKE_ZONE% --project %GCP_PROJECT%; ANSIBLE_CONFIG=ansible/ansible.cfg ansible-playbook ansible/deploy.yml -e image_tag=%IMAGE%:%TAG%"
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
