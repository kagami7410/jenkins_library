import com.common.libraries.*

def call(body){

    def pipelineParams = [:]
    pipeline {

        environment {
            def ZapScanExitCode = 0;
            APPLICATION_NAME = "${pipelineParams.appName != null ? pipelineParams.appName : "reef-forge-frontend"}"
//            APPLICATION_NAME = "${pipelineParams.appName}"
            TARGET_URL = "https://squidcorals.sujantechden.uk"    // Replace with your target application URL
            ZAP_PORT = "8080"
            REPORT_DIR = 'zap_reports'
            REPORT_FILE = 'zap_report.html'
        }

        agent {

            kubernetes{
                inheritFrom 'kube-agent'
                defaultContainer 'agent-container'

                yaml """
                kind: Pod
                metadata:
                  annotations:
                    vault.hashicorp.com/agent-inject: "true"
                    vault.hashicorp.com/role: "jenkins-admin-role"
                    vault.hashicorp.com/agent-inject-secret-stripe-api-key.txt: "kv/stripe/dev/secrets/stripe-api-key" # Vault secret path
                    vault.hashicorp.com/agent-inject-template-stripe-api-key.txt: |
                      {{- with secret "kv/stripe/dev/secrets/stripe-api-key" -}}
                      {{ .Data.data.NEXT_PUBLIC_STRIPE_PUBLIC_KEY }} 
                      {{- end }}
                    vault.hashicorp.com/agent-inject-secret-stripe-api-secret.txt: "kv/stripe/dev/secrets/stripe-api-secret" # Vault secret path
                    vault.hashicorp.com/agent-inject-template-stripe-api-secret.txt: |
                      {{- with secret "kv/stripe/dev/secrets/stripe-api-secret" -}}
                      {{ .Data.data.STRIPE_SECRET_KEY }}
                      {{- end }}
                    vault.hashicorp.com/secret-volume-path: "/app/vault/secrets"

                spec:
                    serviceAccountName: jenkins-admin
                    containers:
                    - name: agent-container
                      image: sujan7410/kubernetes-agent:v1.0.2
                      command: [ "cat"]
                      tty: true
                      volumeMounts:
                      - name: docker-sock-volume
                        mountPath: /var/run/docker.sock
                        readOnly: false
                    - name: node18-container
                      image: node:18
                      command: [ "cat"]
                      volumeMounts:
                      - name: vault-secret
                        mountPath: /vault/secrets
                      tty: true                          
  
                    volumes:
                    - name: docker-sock-volume
                      hostPath:
                        path: "/var/run/docker.sock"
                    - name: vault-secret
                      emptyDir: {}
                    """
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

                                GIT_SSH_COMMAND="ssh -i ${SSH_KEY} git clone git@github.com:kagami7410/${env.APPLICATION_NAME}.git"
                                def version = sh (
                                        script: "node -p \"require('./package.json').version\"",
                                        returnStdout: true
                                ).trim()
                                echo "App Version: ${version}"

                            }

                            sh """
                            echo adding secrets to workspace.....
                            ls  /vault/secrets
                            cp -r /vault/secrets/* ./
                            """


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
                       new helm().deploy(env.APPLICATION_NAME)
                   }
               }
           }


            // stage('Run ZAP Security Scan'){
            //     steps {
            //         container('zap') {
            //             script {
            //                 // Start ZAP in daemon mode and scan the target URL



            //                 ZapScanExitCode = sh(script:
            //                      """
            //                         python3 /zap/zap-baseline.py \
            //                         -t ${TARGET_URL} \
            //                         -r /${REPORT_DIR}/${REPORT_FILE} \
            //                         -J /${REPORT_DIR}/zap_report.json


            //                       """,
            //                         returnStatus: true)

            //                 sh """
            //                         ls
            //                         pwd
            //                     """


            //                 archiveArtifacts artifacts: "**/*, allowEmptyArchive: true"



            //             }
            //         }
            //     }
            // }


//            stage('Archive Report') {
//                steps {
//                    // Archive the HTML and JSON report in Jenkins
//                    archiveArtifacts artifacts: "**/*, allowEmptyArchive: true"
//                }
//            }
//
//
            stage('Publish Report') {
                steps {
                    script{
                        sh """
                            cd /
                            ls
                            pwd
                           """
                    }


                    // Publish the HTML report for viewing in Jenkins
                    publishHTML([
                            reportDir  : "/zap/${REPORT_DIR}",
                            reportFiles: "${REPORT_FILE}",

                            reportName : 'OWASP ZAP Report',
                            alwaysLinkToLastBuild: true,
                            keepAll    : true,  // Optional: set to true if you want to keep reports for each build
                            allowMissing : false  // Set to true if you want to avoid errors if the report is missing
                    ])
                    script{
                        if (ZapScanExitCode != 0) {
                            echo "ZAP baseline scan failed with exit code: ${exitCode}"
                            currentBuild.result = 'FAILURE' // Mark the build as failed
                            error("Stopping pipeline due to ZAP failure")
                        }
                    }

                }
            }
        }
        post {
            always {
                // Clean up ZAP sessions or any generated files if necessary
                cleanWs()
            }
        }
    }
}

