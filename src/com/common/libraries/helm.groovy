package com.common.libraries

def deploy(appName){
    sh """
        kubectl create namespace ${appName}
        helm uninstall ${appName} -n ${appName}
        helm install ${appName} ./helm_charts/squid_corals_frontend -n ${appName}
    """
}