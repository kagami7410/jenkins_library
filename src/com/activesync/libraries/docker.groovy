package com.activesync.libraries

def dockerBuildAndPush(appName, dockerUserName){
    def dockerImage = "${dockerUserName}/${appName}"
    sh"""
        docker build -t ${appName} .
        docker tag ${appName} ${dockerImage}
        docker push ${dockerImage}
     """
}

def dockerLogin(){
    withCredentials([usernamePassword(credentialsId: '670bb4dd-eda3-460a-a4ed-dec383722ee6', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        docker.withRegistry("docker.io", "${USERNAME}", "${PASSWORD}")
    }
}