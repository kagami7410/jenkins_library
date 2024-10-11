import com.common.libraries.*

def call(body){

    def pipelineParams = [:]
    pipeline {

        environment {
            APPLICATION_NAME = "${pipelineParams.appName != null ? pipelineParams.appName : "squid-corals"}"
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
                        sh """
                           git clone https://github.com/kagami7410/futakai_fe_helm_chart.git
                           cd futakai_fe_helm_chart
                           helm template basicHelmChart
                           helm ls -n futakai-fe
                           helm upgrade futakai-fe basicHelmChart/ -n futakai-fe
                           """

                    }
                }
            }
        }
    }
}

