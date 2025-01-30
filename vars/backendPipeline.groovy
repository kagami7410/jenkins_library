import com.common.libraries.*

def call(body){
    pipeline {
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
//                        git remote add origin git@github.com:kagami7410/better_backend.git

                        withCredentials([sshUserPrivateKey(credentialsId: 'github_key', keyFileVariable: 'SSH_KEY')]) {

                            GIT_SSH_COMMAND="ssh -i ${SSH_KEY} git clone git@github.com:kagami7410/better_backend.git"

                        }

                    }
                }
            }
            stage('build') {
                steps {
                    sh "echo $JAVA_HOME"
                    sleep(200)
                    sh "mvn clean package"
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
                           git clone https://github.com/kagami7410/basic-helm-charts.git
                           helm template basic-helm-charts/basicHelmChart --values basic-helm-charts/basicHelmChart/values.yaml
                           helm upgrade helm-test basic-helm-charts/basicHelmChart --values basic-helm-charts/basicHelmChart/values.yaml
                           """

                    }
                }
            }
        }
    }
}

