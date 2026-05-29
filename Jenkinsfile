pipeline {
    agent any

    tools {
        maven 'Maven3'
        nodejs 'Node18'
    }

    stages {

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm install'
                    bat 'npm run build'
                }
            }
        }

        stage('Copy Frontend') {
            steps {
                bat 'xcopy /E /I /Y frontend\\dist\\angular\\browser\\* backend\\src\\main\\resources\\static\\'
            }
        }

        stage('Build Backend') {
            steps {
                dir('backend') {
                    bat 'mvn clean package -DskipTests'
                }
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                deploy adapters: [
                    tomcat9(
                        credentialsId: 'tomcat-creds',
                        path: '',
                        url: 'http://localhost:9090'
                    )
                ],
                contextPath: '/machcare',
                war: 'backend/target/*.war'
            }
        }
    }
}