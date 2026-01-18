def call() {

    /*
     * Load configuration from resources/mongodb-config.yml
     * This file contains all inputs (no hardcoding)
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
                    git url: config.GIT_REPO
                        branch: Sakshi_Totawar_Ansible 
                }
            }

            /* ============================
             * STAGE 2: USER APPROVAL
             * (Conditional)
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
             * STAGE 3: PLAYBOOK EXECUTION
             * ============================
             */
            stage('Execute MongoDB Ansible Playbook') {
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

        /* ============================
         * POST ACTIONS: NOTIFICATION
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
 * Notification wrapper function
 * Calls helper class in src/
 */
def sendNotification(String channel, String message) {
    notifier.Notification.send(this, channel, message)
}
