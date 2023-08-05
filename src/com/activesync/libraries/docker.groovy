package com.activesync.libraries

def dockerBuildAndPush(appName, dockerUserName){
    def dockerImage = "${dockerUserName}/${appName}"
    sh"""
        docker build -t ${appName} .
        docker tag ${appName} ${dockerImage}
        docker push ${dockerImage}
     """
}