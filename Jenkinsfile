pipeline {
    agent any
    environment {
        DOCKER_IMAGE = "havegrit/driply-payments"
        IMAGE_TAG = "v${BUILD_NUMBER}"
        DEPLOY_SERVER = "shin@${env.DEPLOY_SERVER_IP}"
        COMPOSE_PATH = "docker-compose.yml"
        POSTGRES_PASSWORD = credentials('postgres-password')
    }
        stage('Parallel Setup') {
            parallel {
                stage('Generate Environment Files') {
                    steps {
                        script {
                            // 공통 환경 변수 정의
                            def commonEnvVars = [
                                "IMAGE_TAG=${IMAGE_TAG}",
                                "DB_HOST=postgres",
                                "DB_USERNAME=shin",
                                "DB_PASSWORD=${POSTGRES_PASSWORD}",
                                "KAFKA_HOST=kafka",
                                "KAFKA_PORT=9092"
                            ]

                            // .env 파일 생성
                            sh 'cp .env .env.backup || touch .env.backup'
                            writeFile
                            file: '.env',
                            text: """PROFILE=prod
                            DB_NAME=driply_prod
                            ${commonEnvVars.join('\n')}
                            """

                            // .test.env 파일 생성
                            sh 'cp .test.env .test.env.backup || touch .test.env.backup'
                            writeFile
                            file: '.test.env',
                            text: """PROFILE=test
                            IMAGE_TAG=${IMAGE_TAG}
                            DB_NAME=driply_test
                            ${commonEnvVars.join('\n')}
                            """
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
                                    docker compose -f ${COMPOSE_PATH} exec -T postgres pg_isready -U postgres
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