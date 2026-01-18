def call(Map args = [:]) {
    def config
    stage('Load Config') {
        config = readYaml text: libraryResource(args.configFile)
        echo "Environment: ${config.ENVIRONMENT}"
    }
    stage('Clone Repo') {
    echo "Cloning Mongodb code..."
    dir('ansible-src') {
        git branch: 'Sakshi_Totawar_Ansible',
            url: 'https://github.com/OT-MyGurukulam/Ansible_33.git',
            credentialsId: 'github-creds'
    }
}
    if (config.KEEP_APPROVAL_STAGE == true) {
        stage('User Approval') {
            input message: "Approve Mongodb deployment for ${config.ENVIRONMENT}?"
        }
    }
    stage('Ansible Playbook Execution') {
        echo "Running Ansible for Mongodb..."
        sh """
        ansible-playbook \
          ${config.CODE_BASE_PATH}/site.yml \
          -i ${config.CODE_BASE_PATH}/inventory.ini \
          --private-key=/var/lib/jenkins/ansible_3.pem
        """
    }
    stage('Slack Notification') {
        slackSend(
        channel: config.SLACK_CHANNEL_NAME,
        color: currentBuild.currentResult == 'SUCCESS' ? 'good' : 'danger',
        message: """
Mongodb Deployment Completed
Job: ${env.JOB_NAME}
Build #: ${env.BUILD_NUMBER}
Environment: ${config.ENVIRONMENT}
Result: ${currentBuild.currentResult}
Build URL:
${env.BUILD_URL}
"""
    )
    }
}
