package notifier

class Notification {
    static void send(def steps, String channel, String message) {
        steps.slackSend channel: channel, message: message
    }
}
