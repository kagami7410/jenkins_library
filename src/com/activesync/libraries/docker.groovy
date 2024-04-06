package com.activesync.libraries

def dockerBuildAndPush(dockerHubUserName, imageName, tag){
    def dockerImage = "${dockerHubUserName}/${imageName}"
    sh"""
        docker build -t ${dockerImage}:${tag} .
        docker push ${dockerImage}:${tag} 
     """
}

def dockerLogin(){
    withCredentials([usernamePassword(credentialsId: '670bb4dd-eda3-460a-a4ed-dec383722ee6', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
        sh"""
            docker login --username=${USERNAME} --password=${PASSWORD}  
          """
    }
}