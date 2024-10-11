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

                        withCredentials([sshUserPrivateKey(credentialsId: 'github_key', keyFileVariable: 'SSH_KEY')]) {

                            GIT_SSH_COMMAND="ssh -i ${SSH_KEY} git clone git@github.com:kagami7410/site1_pagination.git"
                            def version = sh(returnStdout: true, script: 'jq -r ".version" package.json').trim()
                            echo "React App Version: ${version}"

                        }

                    }
                }
            }


            stage('docker build and push') {
                steps {
                    script{
                        new docker().dockerLogin()
                        new docker().dockerBuildAndPush("sujan7410", "futakai", "latest")
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

