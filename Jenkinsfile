pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "havegrit/driply-payments"
        IMAGE_TAG = "v${BUILD_NUMBER}"
        DEPLOY_SERVER = "shin@${env.DEPLOY_SERVER_IP}"
        COMPOSE_PATH = "docker-compose.yml"
        POSTGRES_PASSWORD = credentials('postgres-password')
        DOCKER_BUILDKIT = "1"
    }
    stages{
        stage('Parallel Setup') {
            parallel {
                stage('Generate Environment Files') {
                    steps {
                        script {
                            // 공통 환경 변수 정의
                            def commonEnvVars = [
                                "IMAGE_TAG=${IMAGE_TAG}"
                            ]

                            // .env 파일 생성
                            sh 'cp .env .env.backup || touch .env.backup'
                            writeFile(
                            file: '.env',
                            text: """PROFILE=prod
DB_HOST=postgres
KAFKA_HOST=kafka
DB_NAME=driply_prod
${commonEnvVars.join('\n')}
"""
                            )

                            // .test.env 파일 생성
                            sh 'cp .test.env .test.env.backup || touch .test.env.backup'
                            writeFile(
                            file: '.test.env',
                            text: """PROFILE=test
DB_HOST=localhost
KAFKA_HOST=localhost
DB_NAME=driply_test
${commonEnvVars.join('\n')}
"""
                            )
                            sh 'echo DB_PASSWORD=$POSTGRES_PASSWORD >> .env'
                            sh 'echo DB_PASSWORD=$POSTGRES_PASSWORD >> .test.env'
                        }
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
        stage('Health Checks') {
            parallel {
                stage('Check Postgres') {
                    steps {
                        script {
                            retry(3) {
                                sh '''
                                    echo "Checking Postgres..."
                                    docker compose -f ${COMPOSE_PATH} exec -T postgres pg_isready -U shin
                                '''
                            }
                        }
                    }
                }
                stage('Check Kafka') {
                    steps {
                        script {
                            retry(3) {
                                sh '''
                                    echo "Checking Kafka..."
                                    docker compose -f ${COMPOSE_PATH} exec -T kafka nc -z localhost 9092
                                '''
                            }
                        }
                    }
                }
            }
        }
        stage('Build') {
            steps {
                script {
                    sh '''
                        ./gradlew clean build \
                            --build-cache \
                            --parallel \
                            --daemon \
                            --stacktrace
                    '''
                }
            }
        }
        stage('Build & Push Docker Image') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'

                        sh """
                            docker buildx build \
                                --platform=linux/amd64 \
                                --cache-from=type=registry,ref=${DOCKER_IMAGE}:cache \
                                --cache-to=type=registry,ref=${DOCKER_IMAGE}:cache,mode=max \
                                --push \
                                -t ${DOCKER_IMAGE}:${IMAGE_TAG} \
                                -t ${DOCKER_IMAGE}:latest \
                                .
                        """
                    }
                }
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
    }
    post {
        always {
            script {
                sh 'docker logout || true'

                sh '''
                    # 사용하지 않는 이미지만 정리
                    docker image prune -af --filter "until=24h"

                    # 빌드 관련 임시 파일 정리
                    docker builder prune -af --filter "until=24h"
                '''
            }
        }
        failure {
            sh '''
                cp .env.backup .env
                cp .test.env.backup .test.env
            '''
            echo 'Pipeline failed!'
        }
        success {
            script {
                sh '''
                    rm -f .env.backup .test.env.backup
                '''
            }
        }
    }
}