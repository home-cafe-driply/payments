pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "havegrit/driply-payments"
        IMAGE_TAG = "v${BUILD_NUMBER}"
        DEPLOY_SERVER = "shin@${env.DEPLOY_SERVER_IP}"
        COMPOSE_PATH = "/var/lib/jenkins/workspace/driply-payments/docker-compose.yml"
        POSTGRES_PASSWORD = credentials('postgres-password')
    }
    stages {
        stage('Generate .env') {
            steps {
                sh '''
                    cp .env .env.backup
                    echo PROFILE=prod > .env
                    echo IMAGE_TAG=$IMAGE_TAG >> .env
                    echo DB_HOST=postgres >> .env
                    echo DB_NAME=driply_prod >> .env
                    echo DB_USERNAME=shin >> .env
                    echo DB_PASSWORD=$POSTGRES_PASSWORD >> .env
                    echo KAFKA_HOST=kafka >> .env
                    echo KAFKA_PORT=9092 >> .env
                '''
            }
        }
        stage('Checkout') {
            steps {
                checkout scm
            }
        }
        stage('Create Secret yml') {
            steps {
                sh 'rm -f src/main/resources/application-secret.yml'
                withCredentials([file(credentialsId: 'application-secret', variable: 'SECRET_YML')]) {
                    sh '''
                        cp $SECRET_YML src/main/resources/application-secret.yml
                        chmod 600 src/main/resources/application-secret.yml
                    '''
                }
            }
        }
        stage('Build') {
            steps {
                sh './gradlew clean build'
            }
        }
        stage('Build Image') {
            steps {
                sh "docker buildx build --platform=linux/amd64 -t ${DOCKER_IMAGE}:${IMAGE_TAG} ."
            }
        }
        stage('Push Image') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                    sh "docker push ${DOCKER_IMAGE}:${IMAGE_TAG}"
                }
            }
        }
        stage('Check Postgres') {
            steps {
                sh '''
                    echo "Checking Postgres..."
                    docker compose -f ${COMPOSE_PATH} exec -T postgres pg_isready -U postgres
                '''
            }
        }
        stage('Check Kafka') {
            steps {
                sh '''
                    echo "Checking Kafka..."
                    docker compose -f ${COMPOSE_PATH} exec -T kafka nc -z localhost 9092
                '''
            }
        }
        stage('Deploy') {
            steps {
                sh """
                    docker compose -f ${COMPOSE_PATH} stop payments || true
                    docker compose -f ${COMPOSE_PATH} rm -f payments || true
                    docker compose -f ${COMPOSE_PATH} up -d --force-recreate --remove-orphans payments
                """
            }
        }
        stage('Logout') {
            steps {
                sh 'docker logout || true'
            }
        }
        stage('Cleanup Docker Images') {
            steps {
                sh 'docker image prune -af --filter "until=24h"'
            }
        }
    }
    post {
        failure {
            sh 'cp .env.backup .env'
            echo 'Pipeline failed!'
        }
    }
}