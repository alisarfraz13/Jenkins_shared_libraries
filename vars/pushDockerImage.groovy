// File: vars/pushDockerImage.groovy

def call(Map config) {
    def credentialsId = config.credentialsId
    def imageName = config.imageName
    def buildTag = config.buildTag
    
    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        passwordVariable: 'DOCKER_PASS',
        usernameVariable: 'DOCKER_USER'
    )]) {
        sh """
            echo "🔐 Logging into DockerHub..."
            echo \${DOCKER_PASS} | docker login -u \${DOCKER_USER} --password-stdin
            
            echo "🏷️ Tagging image..."
            docker tag ${imageName}:${buildTag} \${DOCKER_USER}/${imageName}:${buildTag}
            docker tag ${imageName}:${buildTag} \${DOCKER_USER}/${imageName}:latest
            
            echo "📤 Pushing to DockerHub..."
            docker push \${DOCKER_USER}/${imageName}:${buildTag}
            docker push \${DOCKER_USER}/${imageName}:latest
            
            echo "🚪 Logging out from DockerHub..."
            docker logout
            
            echo "✅ Image pushed successfully"
        """
    }
}
