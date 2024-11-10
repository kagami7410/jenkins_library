import com.common.libraries.*

def call(body){

    def pipelineParams = [:]
    pipeline {

        environment {
            def ZapScanExitCode = 0;
            APPLICATION_NAME = "${pipelineParams.appName != null ? pipelineParams.appName : "squidcorals-frontend"}"
//            APPLICATION_NAME = "${pipelineParams.appName}"
            TARGET_URL = "https://squidcorals.sujantechden.uk"    // Replace with your target application URL
            ZAP_PORT = "8080"
            REPORT_DIR = 'zap_reports'
            REPORT_FILE = 'zap_report.html'
        }

        agent {

            kubernetes{
//                inheritFrom 'kube-agent'
//                defaultContainer 'agent-container'
//                serviceAccount 'jenkins-admin'

                yaml """
                    apiVersion: v1
                    kind: Pod
                    spec:
                        containers:
                        - name: agent-container
                          image: sujan7410/kubernetes-agent:v1.0.2
                          imagePullPolicy: Always
                          command: [ "cat"]
                          tty: true
                          resources:
                            requests:
                              ephemeral-storage: 2Gi
                            limits:
                              ephemeral-storage: 4Gi
                          volumeMounts:
                          - name: docker-sock-volume
                            mountPath: /var/run/docker.sock
                            readOnly: false
                        - name: zap
                          image: ghcr.io/zaproxy/zaproxy:stable
                          command: [ "cat"]
                          tty: true
                          securityContext:
                            runAsUser: 0
                          volumeMounts:
                          - name: extra-volume
                            mountPath: "/zap/wrk"
                            readOnly: false
                        - name: node18-container
                          image: node:18
                          command: [ "cat"]
                          tty: true     
                          resources:
                            requests:
                              ephemeral-storage: 2Gi
                            limits:
                              ephemeral-storage: 4Gi
                        volumes:
                        - name: docker-sock-volume
                          hostPath:
                            path: "/var/run/docker.sock"
                        - name: extra-volume
                          hostPath:
                            path: "/tmp/zap/wrk"
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

                            ZapScanExitCode = sh(script:
                                 """
                                    mkdir -p zap/wrk/zap_reports
                                    chmod +777 zap/wrk/zap_reports
                                    cd zap/wrk/${REPORT_DIR}
                                    echo "<h2>this is testfile!<h2>" > testfile.html
                                    pwd
                                    """,
                                    returnStatus: true)



//                            ZapScanExitCode = sh(script:
//                                 """
//                                    python3 /zap/zap-baseline.py \
//                                    -t ${TARGET_URL} \
//                                    -r /${REPORT_DIR}/${REPORT_FILE} \
//                                    -J /${REPORT_DIR}/zap_report.json
//                                  """,
//                                    returnStatus: true)


                            archiveArtifacts artifacts: "**/*, allowEmptyArchive: true"

                            // Publish the HTML report for viewing in Jenkins
                            publishHTML([
                                        reportDir  : "zap/wrk/${REPORT_DIR}",
                                        reportFiles: "testfile.html",
//                                        reportFiles: "${REPORT_FILE}",

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
            }


//            stage('Archive Report') {
//                steps {
//                    // Archive the HTML and JSON report in Jenkins
//                    archiveArtifacts artifacts: "**/*, allowEmptyArchive: true"
//                }
//            }
//
//
//            stage('Publish Report') {
//                steps {
//
//                    sh """
//
//                        sleep 200
//                       """
//
//                }
//            }
        }
        post {
            always {
                // Clean up ZAP sessions or any generated files if necessary
                cleanWs()
            }
        }
    }
}

