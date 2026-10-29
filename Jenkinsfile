// Windows Jenkins agent. Edit ACR_NAME, then create the Jenkins items listed in README.md.
pipeline {
  agent any
  tools { jdk 'JDK17'; maven 'Maven3' }
  environment {
    ACR_NAME  = 'REPLACE_ACR_NAME'
    IMAGE     = "${ACR_NAME}.azurecr.io/student-task-planner"
    TAG       = "${env.BUILD_NUMBER}"
  }
  stages {
    stage('Checkout')        { steps { checkout scm } }
    stage('Build and test')  {
      steps { bat 'mvn -B clean package' }
      post { always { junit 'target/surefire-reports/*.xml' } }
    }
    stage('Build image')     { steps { bat "docker build -t %IMAGE%:%TAG% -t %IMAGE%:latest ." } }
    stage('Push to ACR')     {
      steps {
        withCredentials([usernamePassword(credentialsId: 'acr-credentials', usernameVariable: 'ACR_USER', passwordVariable: 'ACR_PASS')]) {
          bat 'docker login %ACR_NAME%.azurecr.io -u %ACR_USER% -p %ACR_PASS%'
          bat "docker push %IMAGE%:%TAG%"
          bat "docker push %IMAGE%:latest"
        }
      }
    }
    stage('Deploy to AKS')   {
      steps {
        withCredentials([file(credentialsId: 'aks-kubeconfig', variable: 'KUBECONFIG')]) {
          bat 'kubectl apply -f k8s/deployment.yaml'
          bat "kubectl set image deployment/student-task-planner app=%IMAGE%:%TAG%"
          bat 'kubectl rollout status deployment/student-task-planner --timeout=180s'
        }
      }
    }
  }
}
