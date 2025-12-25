def call(Map config) {
    def credentialsId = config.credentialsId
    def imageName     = config.imageName
    def buildTag      = config.buildTag

    // Creds se Username nikal kr image build kr rhy hein
    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        def fullImageName = "${env.DOCKER_USER}/${imageName}"
        echo "🔨 Building Docker image: ${fullImageName}:${buildTag}"
        
        // Local build (No Pull required later)
        sh "docker build . -t ${fullImageName}:${buildTag}"
        sh "docker tag ${fullImageName}:${buildTag} ${fullImageName}:latest"
    }
}