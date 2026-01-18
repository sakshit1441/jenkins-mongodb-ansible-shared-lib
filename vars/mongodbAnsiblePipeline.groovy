def call() {

    /*
     * Load configuration from resources/mongodb-config.yml
     * Fully config-driven (no hardcoding)
     */
    def config = readYaml text: libraryResource('mongodb-config.yml')

    pipeline {
        agent any

        options {
            timestamps()
        }

        stages {

            /* ============================
             * STAGE 1: CLONE REPOSITORY
             * ============================
             */
            stage('Clone MongoDB Ansible Repo') {
                steps {
                    echo "Cloning MongoDB Ansible repository"
                    echo "Repo   : ${config.GIT_REPO}"
                    echo "Branch : ${config.GIT_BRANCH}"

                    git url: config.GIT_REPO,
                        branch: config.GIT_BRANCH

                    // Debug: confirm branch
                    sh 'git rev-parse --abbrev-ref HEAD'
                }
            }

            /* ============================
             * STAGE 2: USER APPROVAL
             * ============================
             */
            stage('User Approval') {
                when {
                    expression { config.KEEP_APPROVAL_STAGE == true }
                }
                steps {
                    input message: "Approve MongoDB deployment to ${config.ENVIRONMENT} environment?"
                }
            }

            /* ============================
             * STAGE 3: EXECUTE PLAYBOOK
             * ============================
             */
            stage('Execute MongoDB Ansible Playbook') {
                steps {
                    echo "Executing MongoDB Ansible Playbook for ${config.ENVIRONMENT}"

                    sh """
                        cd ${config.CODE_BASE_PATH}
                        ansible-playbook ${config.ANSIBLE_PLAYBOOK} \
                        -i ${config.INVENTORY_FILE}
                    """
                }
            }
        }

        /* ============================
         * POST ACTIONS
         * ============================
         */
        post {
            success {
                sendNotification(
                    config.SLACK_CHANNEL_NAME,
                    "✅ SUCCESS | ${config.ENVIRONMENT} | ${config.ACTION_MESSAGE}"
                )
            }

            failure {
                sendNotification(
                    config.SLACK_CHANNEL_NAME,
                    "❌ FAILED | ${config.ENVIRONMENT} | ${config.ACTION_MESSAGE}"
                )
            }
        }
    }
}

/*
 * Notification helper function
 */
def sendNotification(String channel, String message) {
    notifier.Notification.send(this, channel, message)
}
