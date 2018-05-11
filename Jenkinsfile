pipeline {
    agent {
        kubernetes {
            cloud "kubernetes"
            label "csd"
            containerTemplate {
                name 'jdk'
                image 'java:8-jdk'
                ttyEnabled true
                command 'cat'
            }
        }
    }
    environment {
        VERSION = VersionNumber({
            versionNumberString: '${BUILD_DATE_FORMATTED, "yyyy.MM"}.${BUILDS_THIS_MONTH, XX}'
        })
    }
    stages {
        stage('build') {
            steps {
                container('jdk') {
                    withAWS(credentials: 'jenkins-aws-user') {
                        sh '''
                           sed -i -e "s/0.1.0/${VERSION}/g" descriptor/service.sdl
                           jar cvf HEIMDALI-${VERSION}.jar `find . -mindepth 1 -not -path "./.git*"`
                        '''

                        s3Upload file: "HEIMDALI-${env.VERSION}.jar", bucket: 'csd.jotunn.io', path: "HEIMDALI-${env.VERSION}.jar", acl:'PublicRead'
                   }
                }
            }
        }
    }
    post {
        failure {
            slackSend color: "#a64f36", message: "Heimdali CSD, <${env.BUILD_URL}|build #${BUILD_NUMBER}> Failed"
        }
        success {
            slackSend color: "#36a64f", message: "New CSD Available @here: <http://csd.jotunn.io/HEIMDALI-${VERSION}.jar|HEIMDALI-${VERSION}.jar>"
        }
    }
}
