import com.activesync.libraries.*

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

                            GIT_SSH_COMMAND="ssh -i ${SSH_KEY} git clone git@github.com:kagami7410/site1_pagination.git"

                        }

                    }
                }
            }
            stage('build') {
                steps {
                    script{
                        container("node-18"){
                            sh """
                               npm install
                               npm run build --force
                               """
                        }
                    }
                }
            }

            stage('docker build and push') {
                steps {
                    script{
                        new docker().dockerLogin()
                        new docker().dockerBuildAndPush("better-backend", "sujan7410")
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

