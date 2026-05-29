pipeline {
    agent any

    tools {
        maven 'Maven3'
        nodejs 'Node18'
    }

    stages {

        stage('Checkout') {
            steps {
                git branch: 'main',
                url: 'https://github.com/vikashnayan07/Spring-boot_-_Angular_Project.git'
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    bat 'npm install'
                    bat 'npm run build'
                }
            }
        }

        stage('Copy Frontend to Backend') {
            steps {
                bat 'xcopy /E /I /Y frontend\\dist\\* backend\\src\\main\\resources\\static\\'
            }
        }

        stage('Build Backend WAR') {
            steps {
                dir('backend') {
                    bat 'mvn clean package'
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