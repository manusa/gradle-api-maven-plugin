def logOut = new StringBuilder(), logErr = new StringBuilder()
def dockerLogs = 'docker logs test-proxy'.execute()
dockerLogs.consumeProcessOutput(logOut, logErr)
dockerLogs.waitForOrKill(10_000)
def dockerStop = 'docker stop test-proxy'.execute()
dockerStop.waitForOrKill(30_000)
assert logOut.toString().contains('CONNECT services.gradle.org:443')
def buildLog = new File(basedir, 'build.log').text
assert buildLog.contains('Gradle 8.2.1 download complete')
