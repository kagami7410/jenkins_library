import com.common.libraries.*

def call(body){

    def pipelineParams = [:]
    pipeline {

        environment {
            APPLICATION_NAME = "${pipelineParams.appName != null ? pipelineParams.appName : "squidcorals-frontend"}"
//            APPLICATION_NAME = "${pipelineParams.appName}"
            TARGET_URL = "https://squidcorals.sujantechden.uk"    // Replace with your target application URL
            ZAP_PORT = "8080"
        }

        agent {
            kubernetes{
                inheritFrom 'kube-agent'
                defaultContainer 'agent-container'
                serviceAccount 'jenkins-admin'
            }
        }

        tools {
            maven 'maven'
        }

        stages {
            stage('set up') {
                steps {
                    container('node18-container'){
                        script {

                            withCredentials([sshUserPrivateKey(credentialsId: 'github_key', keyFileVariable: 'SSH_KEY')]) {

                                GIT_SSH_COMMAND="ssh -i ${SSH_KEY} git clone git@github.com:kagami7410/${env.APPLICATION_NAME}-nextjs-fe.git"
                                def version = sh (
                                        script: "node -p \"require('./package.json').version\"",
                                        returnStdout: true
                                ).trim()
                                echo "App Version: ${version}"

                            }
                        }
                    }
                }
            }

//            stage('docker build and push') {
//                steps {
//                    script{
//                        new docker().dockerLogin()
//                        new docker().dockerBuildAndPush("sujan7410", "${env.APPLICATION_NAME}" , "latest")
//                    }
//                }
//            }

//            stage(' deploy to kubernetes '){
//                steps{
//                    script{
//                        new helm().deploy(env.APPLICATION_NAME)
//                    }
//                }
//            }


            stage('Run ZAP Security Scan'){
                steps {
                    container('zap') {
                        script {
                                // Start ZAP in daemon mode and scan the target URL
                                sh """
                                    sleep 5  # Wait for ZAP to fully start
                                    mkdir -p /zap/wrk
                                    chmod -R 755 /zap/wrk
                                    /zap/zap-baseline.py -t${TARGET_URL} -r zap-report.html
                                    cat /zap/wrk/zap-report.html      
                                    """
                        }
                    }
                }
                post {
                    always {
                        // Archive and publish ZAP HTML report
                        archiveArtifacts artifacts: 'zap-report.html', allowEmptyArchive: true
                        publishHTML([allowMissing: true, alwaysLinkToLastBuild: true, keepAll: true,
                                     reportDir: '.', reportFiles: 'zap-report.html', reportName: 'OWASP ZAP Report'])
                    }
                }
            }
        }
    }
}

