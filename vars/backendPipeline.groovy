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
//                        sleep time: 10, unit: 'MINUTES'

//                        withCredentials([sshUserPrivateKey(credentialsId: 'github_key', keyFileVariable: 'SSH_KEY')]) {
                            sh """
                        
                                mkdir project
                                cd project
                                git init
                                rm -rf better_backend
                                git remote add origin git@github.com:kagami7410/better_backend.git
                                git clone git@github.com:kagami7410/better_backend.git

                                """
                            //                                GIT_SSH_COMMAND="ssh -i ${SSH_KEY}" git push

//                        }

                    }
                }
            }
            stage('maven package') {
                steps {
                    sh "mvn clean package"
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