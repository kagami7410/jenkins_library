package com.common.libraries

def deploy(appName){
    sh """
        ls
        helm upgrade --install ${appName} helm_charts -n ${appName} --create-namespace
    """
}