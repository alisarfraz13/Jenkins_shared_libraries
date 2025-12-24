// File: vars/cleanAndDeploy.groovy

def call(Map config) {
    def registryUser = config.registryUser
    def imageName = config.imageName
    def newBuildTag = config.newBuildTag
    def containerName = config.containerName ?: "php-app-container"
    def versionFile = config.versionFile ?: "${WORKSPACE}/.current_version"
    def healthCheckWait = config.healthCheckWait ?: 30  // seconds
    
    try {
        echo "==================== Deployment Started ===================="
        
        // Step 1: Pichla version padho
        def previousBuildTag = "none"
        if (fileExists(versionFile)) {
            previousBuildTag = readFile(versionFile).trim()
            echo "📋 Previous version found: ${previousBuildTag}"
        } else {
            echo "📋 First deployment - no previous version"
        }
        
        // Step 2: Docker compose down (containers band kro, images nahi)
        echo "🛑 Step 1: Stopping containers..."
        sh """
            docker compose down || true
        """
        
        // Step 3: Naya container start kro
        echo "🚀 Step 2: Starting new container with image: ${registryUser}/${imageName}:${newBuildTag}"
        sh """
            docker compose up -d
        """
        
        // Step 4: Health check - naya container properly run ho raha hai yeh check karo
        echo "⏳ Step 3: Running health check (max ${healthCheckWait} seconds)..."
        def healthCheckPass = sh(
            script: """
                for i in \$(seq 1 ${healthCheckWait}); do
                    if docker ps | grep -q ${containerName}; then
                        STATUS=\$(docker inspect ${containerName} --format='{{.State.Status}}' 2>/dev/null || echo "none")
                        if [ "\$STATUS" = "running" ]; then
                            echo "✅ Container is running"
                            exit 0
                        fi
                    fi
                    echo "⏳ Waiting... (\$i/${healthCheckWait}s)"
                    sleep 1
                done
                echo "❌ Container health check failed"
                exit 1
            """,
            returnStatus: true
        )
        
        if (healthCheckPass == 0) {
            echo "✅ Step 4: Container health check PASSED!"
            
            // Ab safe hai - pichla image delete kro kyunke naya sahi chal raha hai
            if (previousBuildTag != "none") {
                echo "🗑️ Step 5: Safely removing old image: ${registryUser}/${imageName}:${previousBuildTag}"
                sh """
                    docker rmi ${registryUser}/${imageName}:${previousBuildTag} -f || true
                """
            }
            
            // Dangling images clean kro
            echo "🧹 Step 6: Cleaning dangling images..."
            sh """
                docker image prune -f
            """
            
            // Current version save kro
            echo "💾 Step 7: Saving current version..."
            sh """
                echo '${newBuildTag}' > ${versionFile}
            """
            
            echo "==================== Deployment Completed Successfully ===================="
            echo "✅ New version deployed: ${registryUser}/${imageName}:${newBuildTag}"
            
        } else {
            echo "❌ Step 4: Container health check FAILED!"
            
            // Rollback - pichla container restart karo agar exist karti ho
            if (previousBuildTag != "none") {
                echo "⚠️ Rolling back to previous version: ${previousBuildTag}"
                sh """
                    docker compose down || true
                    sed -i "s|image: ${registryUser}/${imageName}:.*|image: ${registryUser}/${imageName}:${previousBuildTag}|g" docker-compose.yml
                    docker compose up -d
                """
                error "❌ Deployment FAILED - Rolled back to previous version: ${previousBuildTag}"
            } else {
                error "❌ Deployment FAILED - No previous version available for rollback"
            }
        }
        
    } catch (Exception e) {
        error "❌ Deployment failed: ${e.message}"
    }
}
