def call(Map config) {
    def credentialsId   = config.credentialsId ?: "dockerhub-creds"
    def imageName       = config.imageName
    def newBuildTag     = config.newBuildTag
    def containerName   = config.containerName ?: "php-app-container"
    def versionFile     = config.versionFile ?: "${env.WORKSPACE}/.current_version"
    def healthCheckWait = config.healthCheckWait ?: 30

    withCredentials([usernamePassword(
        credentialsId: credentialsId,
        usernameVariable: 'DOCKER_USER',
        passwordVariable: 'DOCKER_PASS'
    )]) {
        try {
            echo "==================== Deployment Started ===================="

            def previousBuildTag = "none"
            if (fileExists(versionFile)) {
                previousBuildTag = readFile(versionFile).trim()
                echo "📋 Previous version found: ${previousBuildTag}"
            } else {
                echo "📋 First deployment - no previous version"
            }

            echo "🔍 Debug Info:"
            echo "   Registry User: ${env.DOCKER_USER}"
            echo "   Image Name: ${imageName}"
            echo "   New Build Tag: ${newBuildTag}"
            echo "   Previous Tag: ${previousBuildTag}"

            echo "🛑 Step 1: Stopping containers..."
            sh "docker compose down || true"

            echo "🚀 Step 2: Starting new container..."
            sh "docker compose up -d"

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

                if (previousBuildTag != "none") {
                    echo "🗑️ Step 5: Removing old local images..."
                    sh """
                        docker rmi ${env.DOCKER_USER}/${imageName}:${previousBuildTag} -f 2>/dev/null || true
                        docker rmi ${imageName}:${previousBuildTag} -f 2>/dev/null || true
                    """
                }

                echo "🧹 Step 6: Cleaning dangling images..."
                sh "docker image prune -f"

                echo "💾 Step 7: Saving current version..."
                sh "echo '${newBuildTag}' > '${versionFile}'"

                echo "==================== Deployment Completed Successfully ===================="
                echo "✅ New version deployed: ${env.DOCKER_USER}/${imageName}:${newBuildTag}"

            } else {
                echo "❌ Step 4: Container health check FAILED!"

                if (previousBuildTag != "none") {
                    echo "⚠️ Rolling back to previous version: ${previousBuildTag}"
                    sh """
                        docker compose down || true
                        sed -i "s|image: .*|image: ${env.DOCKER_USER}/${imageName}:${previousBuildTag}|g" docker-compose.yml
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
}
