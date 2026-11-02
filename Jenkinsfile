
pipeline {
    agent any

    environment {
        GCP_PROJECT = 'axiomatic-set-443307-k3'
        GCP_REGION = 'asia-south1'
        AR_REPOSITORY = 'student-planner-repo'

        IMAGE = 'asia-south1-docker.pkg.dev/axiomatic-set-443307-k3/student-planner-repo/student-task-planner'
        TAG = "${env.BUILD_NUMBER}"

        GKE_CLUSTER = 'student-planner-cluster'
        GKE_ZONE = 'asia-south1-a'

        // Keep false until the GKE cluster is ready.
        DEPLOY_TO_GKE = 'false'

        MAVEN = 'C:\\Apache\\maven\\bin\\mvn.cmd'
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

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

        stage('Build Docker Image') {
            steps {
                bat 'docker build -t %IMAGE%:%TAG% -t %IMAGE%:latest .'
            }
        }

        stage('Authenticate with Google Cloud') {
            steps {
                withCredentials([
                    file(credentialsId: 'gcp-service-account-json',
                         variable: 'GCP_KEY')
                ]) {
                    bat '''
                        gcloud auth activate-service-account --key-file="%GCP_KEY%"
                        gcloud config set project %GCP_PROJECT%
                        gcloud auth configure-docker asia-south1-docker.pkg.dev --quiet
                    '''
                }
            }
        }

        stage('Push to Artifact Registry') {
            steps {
                bat 'docker push %IMAGE%:%TAG%'
                bat 'docker push %IMAGE%:latest'
            }
        }

        stage('Deploy to GKE') {
            when {
                expression {
                    return env.DEPLOY_TO_GKE == 'true'
                }
            }
            steps {
                echo 'GKE deployment will be configured through Ansible.'
            }
        }

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
    }
}
