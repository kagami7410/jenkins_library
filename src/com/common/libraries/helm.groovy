package com.common.libraries

def deploy(appName){
    sh """
        helm uninstall ${appName} -n ${appName}
        helm install ${appName} ./helm_charts/squid_corals_frontend -n ${appName}
    """
}