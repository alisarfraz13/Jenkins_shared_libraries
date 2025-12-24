def call(Map config) {
    def imageName = config.imageName
    
    echo ""
    echo "╔════════════════════════════════════════════╗"
    echo "║        📊 DEPLOYMENT STATUS CHECK          ║"
    echo "╚════════════════════════════════════════════╝"
    
    sh """
        echo ""
        echo "🐳 Running Containers:"
        docker ps | grep ${imageName} || echo "   No containers running"
        
        echo ""
        echo "🖼️ Docker Images:"
        docker images | grep ${imageName} || echo "   No images found"
        
        echo ""
        echo "💾 Workspace Version File:"
        cat ${WORKSPACE}/.current_version 2>/dev/null || echo "   No version file"
    """
}
