package com.common.libraries

def deploy(appName){
    sh """
        ls
        helm upgrade --install ${appName} helm_charts/reef-forge-helmchart -n ${appName} --create-namespace
    """
}