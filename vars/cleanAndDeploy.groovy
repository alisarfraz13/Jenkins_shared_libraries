def call(Map config) {
    def credentialsId = config.credentialsId
    def imageName     = config.imageName
    def newTag        = config.newBuildTag
    
    withCredentials([usernamePassword(credentialsId: credentialsId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
        try {
            def fullImageName = "${env.DOCKER_USER}/${imageName}"
            
            echo "🚀 Deployment Started for Tag: ${newTag}"

            sh """
                echo "\$DOCKER_PASS" | docker login -u "\$DOCKER_USER" --password-stdin
                
                # Yeh variable docker-compose ko bataye ga k konsi image uthani hai
                export IMAGE_TAG=${newTag}
                
                # SMART DEPLOY: Sirf change hony wala container update hoga
                docker compose up -d
            """
            echo "✅ Deployment Successful"

            // --- CLEANUP SECTION ---
            echo "🧹 Starting Cleanup..."
            
            // 1. Remove dangling images
            sh "docker image prune -f"

            // 2. Remove Previous Build (Current - 1)
            def prevTag = (newTag.toInteger() - 1).toString()
            if (newTag.toInteger() > 1) {
                echo "🗑️ Removing previous version: ${fullImageName}:${prevTag}"
                sh "docker rmi ${fullImageName}:${prevTag} || true"
            }

        } catch (Exception e) {
            echo "❌ Deployment Failed: ${e.message}"
            error "Deployment failed"
        }
    }
}