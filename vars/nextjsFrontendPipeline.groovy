import com.common.libraries.*

def call(body){

    def pipelineParams = [:]
    pipeline {

        environment {
            APPLICATION_NAME = "${pipelineParams.appName != null ? pipelineParams.appName : "squidcorals-frontend"}"
//            APPLICATION_NAME = "${pipelineParams.appName}"

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

            stage('docker build and push') {
                steps {
                    script{
                        new docker().dockerLogin()
                        new docker().dockerBuildAndPush("sujan7410", "${env.APPLICATION_NAME}" , "latest")
                    }
                }
            }

            stage(' deploy to kubernetes '){
                steps{
                    script{
                        // Check if the namespace exists
//                        def nsExists = sh(script: "kubectl get namespaces -o json | jq -r '.items[] | select(.metadata.name==\"${env.APPLICATION_NAME}\") | .metadata.name' | grep ${env.APPLICATION_NAME}", returnStatus: true)
//
//
//                        if (nsExists != 0) {
//                            echo "Namespace ${env.APPLICATION_NAME} does not exist. Creating it now..."
//                            sh "kubectl create namespace ${env.APPLICATION_NAME}"
//                        } else {
//                            echo "Namespace ${env.APPLICATION_NAME} already exists."
//                        }
                        new helm().deploy(env.APPLICATION_NAME)
                    }
                }
            }
        }
    }
}

