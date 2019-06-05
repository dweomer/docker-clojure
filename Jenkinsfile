pipeline {
    agent {
        label 'centos7-docker-4c-2g'
    }

    stages {
        // stage('Checkout') {
        //     steps {
        //         checkout scm
        //     }
        // }
        stage('Build') {
            parallel {
                stage('AMD64') {
                    agent {
                        label 'centos7-docker-4c-2g'
                    }
                    steps {
                        script {
                            sh 'env | sort'
                            sh 'find . | sort'
                            docker.build('clojure:amd64', './target/openjdk-8/alpine/lein/.')
                        }
                    }
                }

                stage('ARM64') {
                    agent {
                        label 'ubuntu18.04-docker-arm64-4c-2g'
                    }
                    steps {
                        script {
                            sh 'env | sort'
                            sh 'find . | sort'
                            docker.build('clojure:arm64', './target/openjdk-8/alpine/lein/.')
                        }
                    }
                }
            }
        }
    }
}
