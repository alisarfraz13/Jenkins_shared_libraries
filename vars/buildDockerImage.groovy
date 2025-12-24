def call(Map config) {
    def imageName = config.imageName
    def buildTag = config.buildTag
    
    try {
        echo "🔨 Building Docker image: ${imageName}:${buildTag}"
        sh """
            docker build . -t ${imageName}:${buildTag}
            docker tag ${imageName}:${buildTag} ${imageName}:latest
            echo "✅ Image built successfully"
        """
    } catch (Exception e) {
        error "❌ Docker build failed: ${e.message}"
    }
}
