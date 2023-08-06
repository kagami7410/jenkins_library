import com.activesync.libraries.*

def call(body){
    pipeline {
        agent {
            kubernetes{
                inheritFrom 'kube-agent'
                defaultContainer 'agent-container'
                serviceAccount 'jenkins-deploy-admin'
            }
        }

        tools {
            maven 'maven'
        }

        stages {
//            stage('set up') {
//                steps {
//                    sh 'rm -rf better_backend'
//                    sh 'git clone https://github.com/kagami7410/better_backend.git '
//                }
//            }
//
//            stage('maven package') {
//                steps {
//                    sh "mvn clean package"
//                }
//            }
//
//            stage('docker build and push') {
//                steps {
//                    script{
//                        new docker().dockerLogin()
//                        new docker().dockerBuildAndPush("better-backend", "sujan7410")
//                    }
//                }
//            }


            stage(' deploy to kubernetes '){
                steps{
                    script{
                        sh """
                           git clone https://github.com/kagami7410/basic-helm-charts.git
                           helm template basic-helm-charts/basicHelmChart --values basic-helm-charts/basicHelmChart/values.yaml
                           helm install helm-test basic-helm-charts/basicHelmChart --values basic-helm-charts/basicHelmChart/values.yaml
                           """

                    }
                }
            }
        }
    }
}