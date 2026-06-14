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
        stage('Prepare Workspace') {
            steps {
                sh '''
                    set +x
                    set -eu
                    sudo chown -R "$(id -un):$(id -gn)" "$WORKSPACE/backend" "$WORKSPACE/frontend" || true
                    rm -rf backend/target frontend/dist frontend/.angular/cache
                    chmod +x backend/mvnw
                '''
            }
        }

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
                    sh 'npx -y -p node@20.19.0 node ./node_modules/@angular/cli/bin/ng build --configuration production'
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
                        sh '''
                            set +x
                            set -eu
                            [ ! -f /etc/profile.d/machcare-ci-env.sh ] || . /etc/profile.d/machcare-ci-env.sh
                            ./mvnw test
                        '''
                    }
                }
            }
        }

        stage('Build Backend WAR') {
            steps {
                dir('backend') {
                    sh '''
                        set +x
                        set -eu
                        [ ! -f /etc/profile.d/machcare-ci-env.sh ] || . /etc/profile.d/machcare-ci-env.sh
                        ./mvnw clean package -DskipTests
                    '''
                }
            }
        }

        stage('Deploy To Tomcat') {
            steps {
                sh '''
                    set +x
                    rm -f .jenkins-deployed
                    sudo /usr/local/bin/machcare-deploy deploy "$WAR_PATH"
                    touch .jenkins-deployed
                '''
            }
        }

        stage('Verify Production') {
            steps {
                sh '''
                    set +x
                    set -eu
                    [ ! -f /etc/profile.d/machcare-ci-env.sh ] || . /etc/profile.d/machcare-ci-env.sh
                    curl -fsSI "$APP_URL/" >/dev/null
                    curl -fsSI "$APP_URL/machcare/" >/dev/null
                    INDEX_HTML="$(curl -fsS "$APP_URL/machcare/")"
                    printf '%s' "$INDEX_HTML" | grep -q 'src="/machcare/main-'
                    printf '%s' "$INDEX_HTML" | grep -q 'href="/machcare/styles-'
                    MAIN_JS="$(printf '%s' "$INDEX_HTML" | sed -n 's/.*src="\\([^"]*main-[^"]*\\.js\\)".*/\\1/p' | head -n 1)"
                    printf '%s' "$INDEX_HTML" \
                        | grep -o '/machcare/[^" ]*\\.\\(js\\|css\\)' \
                        | sort -u \
                        | while read -r asset; do
                            curl -fsS "$APP_URL$asset" -o /dev/null
                        done
                    if [ -n "${SMOKE_LOGIN_EMAIL:-}" ] && [ -n "${SMOKE_LOGIN_PASSWORD:-}" ]; then
                        LOGIN_PAYLOAD="$(python3 - <<'PY'
import json
import os
print(json.dumps({
    "email": os.environ["SMOKE_LOGIN_EMAIL"],
    "password": os.environ["SMOKE_LOGIN_PASSWORD"],
}))
PY
)"
                        curl -fsS -X POST "$APP_URL/machcare/api/auth/login" \
                            -H 'Content-Type: application/json' \
                            --data "$LOGIN_PAYLOAD" \
                            | grep -q '"success":true'
                    fi
                '''
            }
        }
    }

    post {
        success {
            echo "MachCare deployed successfully: ${env.APP_URL}"
        }
        failure {
            script {
                if (fileExists('.jenkins-deployed')) {
                    sh 'sudo /usr/local/bin/machcare-deploy rollback || true'
                }
            }
            echo 'MachCare deployment failed. Check the stage log above.'
        }
    }
}
