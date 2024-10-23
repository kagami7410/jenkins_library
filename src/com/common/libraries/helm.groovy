package com.common.libraries

def deploy(appName, namespace){
    sh """
        helm install ${appName} ./helm_charts/squid_corals_frontend -n ${namespace}
    """
}