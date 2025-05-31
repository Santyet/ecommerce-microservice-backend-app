pipeline {
    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'stage', 'prod'],
            description: 'Ambiente de despliegue'
        )
    }

    environment {
        DOCKER_NAMESPACE = "kenbra"
        K8S_NAMESPACE = "default"
        SELECTED_ENV = "${params.ENVIRONMENT}"
    }

    stages {

        stage('Setup') {
            steps {
                sh '''
                export PATH=$HOME/bin:$PATH

                if ! command -v kubectl &> /dev/null; then
                    mkdir -p $HOME/bin
                    curl -s https://dl.k8s.io/release/stable.txt
                    curl -LO https://dl.k8s.io/release/<html>
                    <head><title>302 Found</title></head>
                    <body>
                    <center><h1>302 Found</h1></center>
                    <hr><center>nginx</center>
                    </body>
                    </html>
                    /bin/linux/amd64/kubectl
                    chmod +x kubectl && mv kubectl $HOME/bin/
                fi

                if [ ! -d $HOME/java11 ]; then
                    curl -L -o /tmp/openjdk-11.tar.gz https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz
                    tar -xzf /tmp/openjdk-11.tar.gz -C $HOME
                    mv $HOME/jdk-11.0.2 $HOME/java11
                fi

                export JAVA_HOME=$HOME/java11
                export PATH=$JAVA_HOME/bin:$PATH

                if ! command -v mvn &> /dev/null; then
                    curl -sL https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz | tar -xz -C $HOME
                    mv $HOME/apache-maven-3.8.6 $HOME/maven
                fi

                export PATH=$HOME/maven/bin:$PATH
                '''
            }
        }

        stage('Unit Tests') {
            when {
                environment name: 'SELECTED_ENV', value: 'stage'
            }
            steps {
                sh '''
                export JAVA_HOME=$HOME/java11
                export PATH=$JAVA_HOME/bin:$HOME/maven/bin:$PATH
                cd product-service
                mvn clean test -Dmaven.test.failure.ignore=true
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Integration Tests') {
            when {
                environment name: 'SELECTED_ENV', value: 'stage'
            }
            steps {
                sh '''
                export PATH=$HOME/maven/bin:$PATH
                cd product-service
                mvn test -Dtest=*Integration* -Dmaven.test.failure.ignore=true
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*Integration*.xml'
                }
            }
        }

        stage('Deploy Services') {
            steps {
                sh '''
                export PATH=$HOME/bin:$PATH

                find k8s -name "*.yaml" | xargs sed -i "s|image: selimhorri/|image: $DOCKER_NAMESPACE/|g"

                kubectl apply -f k8s/api-gateway.yaml
                sleep 30

                for svc in order payment product shipping user favourite proxy-client; do
                    kubectl apply -f k8s/${svc}-service.yaml
                done

                sleep 60
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                export PATH=$HOME/bin:$PATH
                kubectl get pods
                kubectl get services

                IP=$(kubectl get service api-gateway -o jsonpath='{.spec.clusterIP}')
                curl -s $IP:8080/actuator/health || echo "API Gateway aún no disponible"
                '''
            }
        }

        stage('Generar Release Notes') {
            when {
                expression { return env.SELECTED_ENV == 'prod' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: '5f03ddc6-58de-4141-bdb5-bade9a59a7c3', usernameVariable: 'GH_USER', passwordVariable: 'GH_TOKEN')]) {
                    script {                        echo "Generating Release Notes for PROD environment..."
                        def now = new Date()
                        // Format: vYEAR.MONTH.DAY.HOURMINUTE (e.g., v2023.05.15.1430)
                        def tag = "v${now.format('yyyy.MM.dd.HHmm')}"
                        def title = "🚀 Production Release ${tag}"
                        def releaseDate = now.format('MMMM dd, yyyy \'at\' HH:mm')
                        
                        // Create formatted release notes
                        def releaseNotes = """
🧾 **Resumen de versión**
- Fecha: ${new Date().format('yyyy-MM-dd HH:mm')}
- Último commit: ${commit}
- Detalle: ${msg}

🔧 Servicios desplegados:
API Gateway, Order, Payment, Product, User, Shipping, Favourite, Proxy-client

✅ Estado: Despliegue exitoso en producción
"
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
        success {
            echo "✅ Pipeline finalizado en ambiente ${env.SELECTED_ENV}"
        }
        failure {
            echo "❌ Error en la ejecución del pipeline para ${env.SELECTED_ENV}"
        }
    }
}
