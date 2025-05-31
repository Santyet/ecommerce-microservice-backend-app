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

        stages {

        stage('Setup') {
            steps {
                sh '''
                export PATH=$HOME/bin:$PATH

                if ! command -v kubectl &> /dev/null; then
                    echo "kubectl no encontrado, instalando..."
                    mkdir -p $HOME/bin
                    # Captura la versión estable en una variable
                    KUBECTL_STABLE_VERSION=$(curl -s https://dl.k8s.io/release/stable.txt)
                    echo "Versión estable de kubectl: ${KUBECTL_STABLE_VERSION}"
                    # Usa la variable para construir la URL de descarga
                    curl -LO "https://dl.k8s.io/release/${KUBECTL_STABLE_VERSION}/bin/linux/amd64/kubectl"
                    chmod +x kubectl && mv kubectl $HOME/bin/
                    echo "kubectl instalado en $HOME/bin/"
                else
                    echo "kubectl ya está instalado."
                fi

                if [ ! -d $HOME/java11 ]; then
                    echo "Directorio Java 11 no encontrado, instalando..."
                    curl -L -o /tmp/openjdk-11.tar.gz https://download.java.net/java/GA/jdk11/9/GPL/openjdk-11.0.2_linux-x64_bin.tar.gz
                    tar -xzf /tmp/openjdk-11.tar.gz -C $HOME
                    mv $HOME/jdk-11.0.2 $HOME/java11
                    echo "Java 11 instalado en $HOME/java11"
                else
                    echo "Java 11 ya está instalado."
                fi

                export JAVA_HOME=$HOME/java11
                export PATH=$JAVA_HOME/bin:$PATH

                if ! command -v mvn &> /dev/null; then
                    echo "Maven no encontrado, instalando..."
                    curl -sL https://archive.apache.org/dist/maven/maven-3/3.8.6/binaries/apache-maven-3.8.6-bin.tar.gz | tar -xz -C $HOME
                    mv $HOME/apache-maven-3.8.6 $HOME/maven
                    echo "Maven instalado en $HOME/maven"
                else
                    echo "Maven ya está instalado."
                fi

                export PATH=$HOME/maven/bin:$PATH
                echo "PATH configurado: $PATH"
                echo "JAVA_HOME configurado: $JAVA_HOME"
                # Opcional: verificar versiones
                # mvn -version
                # kubectl version --client
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
                // Usar expression para comparar strings es más robusto
                expression { return env.SELECTED_ENV == 'prod' }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: '5f03ddc6-58de-4141-bdb5-bade9a59a7c3', usernameVariable: 'GH_USER', passwordVariable: 'GH_TOKEN')]) {
                    script {
                        echo "Generating Release Notes for PROD environment..."
                        def now = new Date()
                        def tag = "v${now.format('yyyy.MM.dd.HHmm')}"
                        def title = "🚀 Production Release ${tag}"
                        
                        // Obtener commit y mensaje del commit
                        def commit = sh(script: "git rev-parse --short HEAD", returnStdout: true).trim()
                        def msg = sh(script: "git log -1 --pretty=%B", returnStdout: true).trim()

                        def releaseNotes = """
🧾 **Resumen de versión**
- Fecha: ${now.format('yyyy-MM-dd HH:mm')}
- Último commit: ${commit}
- Detalle: ${msg}

🔧 Servicios desplegados:
API Gateway, Order, Payment, Product, User, Shipping, Favourite, Proxy-client

✅ Estado: Despliegue exitoso en producción
"""
                        echo "Contenido de las notas de release:"
                        echo releaseNotes

                        // Comandos Git y GitHub CLI
                        sh """
                            echo "Configurando Git..."
                            git config user.email "ci@auto-release.com"
                            git config user.name "Jenkins CI"
                            # La variable GH_TOKEN (que contiene tu token) se usa aquí
                            git config --global url."https://oauth2:${GH_TOKEN}@github.com/".insteadOf "https://github.com/"

                            echo "Creando tag ${tag}..."
                            git tag ${tag} -m "Release ${tag} generado automáticamente"
                            echo "Haciendo push del tag ${tag}..."
                            git push origin ${tag}

                            echo "Creando release en GitHub para el tag ${tag}..."
                            gh release create ${tag} --title "${title}" --notes "${releaseNotes}"
                        """
                        echo "Release notes generadas y publicadas para ${tag}"
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
