def call() {

    // Load configuration
    def config = readYaml text: libraryResource('mongodb-config.yml')

    pipeline {
        agent any

        stages {

            stage('Clone MongoDB Ansible Repo') {
                steps {
                    echo "Cloning MongoDB Ansible repository"
                    git url: config.GIT_REPO
                }
            }

            stage('User Approval') {
                when {
                    expression { config.KEEP_APPROVAL_STAGE == true }
                }
                steps {
                    input message: "Approve MongoDB deployment to ${config.ENVIRONMENT}?"
                }
            }

            stage('MongoDB Playbook Execution') {
                steps {
                    echo "Executing MongoDB Ansible Playbook"
                    sh """
                    cd ${config.CODE_BASE_PATH}
                    ansible-playbook ${config.ANSIBLE_PLAYBOOK} \
                    -i ${config.INVENTORY_FILE}
                    """
                }
            }
        }

        post {
            success {
                sendNotification(
                    config.SLACK_CHANNEL_NAME,
                    "✅ SUCCESS: ${config.ACTION_MESSAGE}"
                )
            }
            failure {
                sendNotification(
                    config.SLACK_CHANNEL_NAME,
                    "❌ FAILED: ${config.ACTION_MESSAGE}"
                )
            }
        }
    }
}
