import com.activesync.libraries.*

def call(body){
    pipeline {
        agent {
            kubernetes{
                yaml '''
                apiVersion: v1
                kind: Pod
                spec:
                    containers:
                    - name: agent-container
                      image: sujan7410/docker_java_helm:v1.0.0
                      command:
                      - cat
                      tty: true
                      volumeMounts:
                      - name: docker-sock-volume
                        mountPath: /var/run/docker.sock
                        readOnly: false
                       
                    volumes:
                    - name: docker-sock-volume
                      hostPath:
                        path: "/var/run/docker.sock"
                        
                  
                      
                    '''
            }
        }

        tools {
            maven 'maven'
        }

        stages {
            stage('set up') {
                steps {
                    sh 'rm -rf better_backend'
                    sh 'git clone https://github.com/kagami7410/better_backend.git '
                    sh 'java -version'
                }
            }

            stage('maven package') {
                steps {
                    script{
                        container("agent-container")
                                {
                                    //                     def mvnHOME = tool name: 'maven', type: 'maven'
                                    sh 'java -version'
                                    sh "mvn -version"
                                    sh 'mvn compile'
                                    sh "mvn clean package"
                                    sh 'docker pull openjdk:17'
//                            sh 'helm template basic-helm-charts/basicHelmChart --valuesbasic-helm-charts/basicHelmChart/values.yaml  '
                                    sh 'helm create test_template'
                                }
                    }
                }
            }

            stage('test library '){
                steps{
                    new helloWorld().helloWorld()
                }
            }
        }
    }
}