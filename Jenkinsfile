pipeline {
    agent any

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '15'))
    }

    triggers {
        pollSCM('H/2 * * * *')
    }

    environment {
        APP_URL = 'https://machcare.me'
        WAR_PATH = 'backend/target/ROOT.war'
        SPRING_PROFILES_ACTIVE = 'prod'
    }

    stages {
        stage('Install Frontend Dependencies') {
            steps {
                dir('frontend') {
                    sh 'npm ci'
                }
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm run build'
                    sh '! grep -R "localhost:9090\\|localhost:8080" dist/angular/browser'
                }
            }
        }

        stage('Package Frontend Assets') {
            steps {
                sh '''
                    set -eu
                    find backend/src/main/resources/static -maxdepth 1 -type f \\( \
                        -name 'main-*.js' -o \
                        -name 'chunk-*.js' -o \
                        -name 'polyfills-*.js' -o \
                        -name 'styles-*.css' -o \
                        -name 'index.html' -o \
                        -name 'favicon.ico' \
                    \\) -delete
                    rm -rf backend/src/main/resources/static/landing
                    cp -R frontend/dist/angular/browser/. backend/src/main/resources/static/
                '''
            }
        }

        stage('Run Backend Tests') {
            steps {
                dir('backend') {
                    sh 'chmod +x mvnw'
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        sh './mvnw test'
                    }
                }
            }
        }

        stage('Build Backend WAR') {
            steps {
                dir('backend') {
                    sh './mvnw clean package -DskipTests'
                }
            }
        }

        stage('Deploy To Tomcat') {
            steps {
                sh 'sudo /usr/local/bin/machcare-deploy "$WAR_PATH"'
            }
        }

        stage('Verify Production') {
            steps {
                sh '''
                    set -eu
                    curl -fsSI "$APP_URL/" >/dev/null
                    curl -fsSI "$APP_URL/machcare/" >/dev/null
                    curl -fsS -X POST "$APP_URL/machcare/api/auth/login" \
                        -H 'Content-Type: application/json' \
                        --data '{"email":"vikash@gmail.com","password":"Admin@123"}' \
                        | grep -q '"success":true'
                '''
            }
        }
    }

    post {
        success {
            echo "MachCare deployed successfully: ${env.APP_URL}"
        }
        failure {
            echo 'MachCare deployment failed. Check the stage log above.'
        }
    }
}
