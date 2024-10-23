package com.common.libraries

def deploy(appName, namespace){
    sh """
        helm upgrade --install ${appName} ./helm_charts/squid_corals_frontend -n ${appName}-frontend
    """
}