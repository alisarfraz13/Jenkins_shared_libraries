def call(String repoUrl, String branch) {
    checkout([$class: 'GitSCM', 
        branches: [[name: branch]], 
        userRemoteConfigs: [[url: repoUrl]]
    ])
    }